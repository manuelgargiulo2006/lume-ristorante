/* =====================================================================
   Lume · gestionale
   JavaScript senza librerie: ogni funzione si attiva solo se nella pagina
   c'è l'elemento con l'attributo data-* corrispondente.
   ===================================================================== */
(() => {
    'use strict';

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
    const icona = nome => `<svg class="i" aria-hidden="true"><use href="${Lume.icone}#${nome}"/></svg>`;

    // stesso formato del server: "€ 1.234,50"
    const due = new Intl.NumberFormat('it-IT', { minimumFractionDigits: 2, maximumFractionDigits: 2, useGrouping: 'always' });
    const zero = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 0, useGrouping: 'always' });
    const euro = { format: v => '€ ' + due.format(v) };
    const euroTondo = { format: v => '€ ' + zero.format(v) };
    const numero = new Intl.NumberFormat('it-IT', { useGrouping: 'always' });

    /* ---------- utilità condivise, esposte anche alle singole pagine ---------- */
    const Lume = window.Lume = {
        icone: '',
        euro: v => euro.format(Number(v) || 0),
        euroTondo: v => euroTondo.format(Number(v) || 0),

        csrf() {
            return {
                [$('meta[name=_csrf_header]')?.content || 'X-CSRF-TOKEN']: $('meta[name=_csrf]')?.content || ''
            };
        },

        /** POST verso un'API JSON del gestionale. */
        async post(url, dati = {}) {
            const corpo = new URLSearchParams();
            Object.entries(dati).forEach(([k, v]) => v != null && corpo.append(k, v));
            const r = await fetch(url, {
                method: 'POST',
                headers: { Accept: 'application/json', ...Lume.csrf() },
                body: corpo
            });
            if (r.status === 401) { location.reload(); throw new Error('Sessione scaduta'); }
            const json = await r.json().catch(() => ({}));
            if (!r.ok) throw new Error(json.errore || 'Operazione non riuscita');
            return json;
        },

        async get(url) {
            const r = await fetch(url, { headers: { Accept: 'application/json' } });
            if (r.status === 401) { location.reload(); throw new Error('Sessione scaduta'); }
            if (!r.ok) throw new Error('Errore ' + r.status);
            return r.json();
        },

        toast(testo, tipo = 'ok') {
            const box = $('[data-toasts]');
            if (!box || !testo) return;
            const t = document.createElement('div');
            t.className = `toast toast--${tipo}`;
            t.setAttribute('role', tipo === 'errore' ? 'alert' : 'status');
            t.innerHTML = `<span class="toast__icon">${icona(tipo === 'errore' ? 'avviso' : 'check')}</span><span></span>`;
            t.lastElementChild.textContent = testo;
            box.append(t);
            const via = () => { t.classList.add('is-out'); setTimeout(() => t.remove(), 300); };
            t.addEventListener('click', via);
            setTimeout(via, tipo === 'errore' ? 6500 : 3800);
        },

        /** Legge un blocco <script type="application/json"> dentro un elemento. */
        dati(el) {
            const s = el?.querySelector('script[type="application/json"]');
            try { return s ? JSON.parse(s.textContent) : null; } catch (e) { return null; }
        },

        escape(s) {
            return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
        }
    };

    /* =================================================================
       Struttura: tema, menù laterale, notifiche, orologio, palette
       ================================================================= */

    function tema() {
        $('[data-tema]')?.addEventListener('click', () => {
            const scuroAdesso = document.documentElement.dataset.theme
                ? document.documentElement.dataset.theme === 'dark'
                : matchMedia('(prefers-color-scheme: dark)').matches;
            const nuovo = scuroAdesso ? 'light' : 'dark';
            document.documentElement.dataset.theme = nuovo;
            try { localStorage.setItem('lume-tema', nuovo); } catch (e) { /* niente */ }
            document.dispatchEvent(new CustomEvent('lume:tema'));
        });
    }

    function laterale() {
        const app = $('[data-app]');
        if (!app) return;
        $('[data-side-open]')?.addEventListener('click', () => app.classList.add('side-open'));
        $('[data-side-close]')?.addEventListener('click', () => app.classList.remove('side-open'));
        document.addEventListener('keydown', e => { if (e.key === 'Escape') app.classList.remove('side-open'); });
    }

    function popover() {
        $$('[data-pop]').forEach(pop => {
            const btn = $('[data-pop-toggle]', pop);
            btn.addEventListener('click', e => {
                e.stopPropagation();
                const aperto = pop.classList.toggle('is-open');
                btn.setAttribute('aria-expanded', String(aperto));
            });
            document.addEventListener('click', e => {
                if (!pop.contains(e.target)) { pop.classList.remove('is-open'); btn.setAttribute('aria-expanded', 'false'); }
            });
        });
    }

    function orologio() {
        const el = $('[data-orologio]');
        if (!el) return;
        const f = new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit' });
        const tick = () => { el.textContent = f.format(new Date()); };
        tick();
        setInterval(tick, 15000);
    }

    function palette() {
        const dlg = $('[data-palette]');
        if (!dlg) return;
        const input = $('[data-palette-input]', dlg);
        const voci = $$('[data-palette-list] a', dlg);
        const vuoto = $('[data-palette-empty]', dlg);
        let attiva = 0;

        const visibili = () => voci.filter(a => !a.parentElement.hidden);
        const evidenzia = i => {
            const v = visibili();
            attiva = (i + v.length) % Math.max(v.length, 1);
            voci.forEach(a => a.classList.remove('is-active'));
            v[attiva]?.classList.add('is-active');
            v[attiva]?.scrollIntoView({ block: 'nearest' });
        };
        const filtra = () => {
            const q = input.value.trim().toLowerCase();
            voci.forEach(a => {
                const testo = (a.textContent + ' ' + (a.dataset.k || '')).toLowerCase();
                a.parentElement.hidden = q && !q.split(/\s+/).every(p => testo.includes(p));
            });
            vuoto.hidden = visibili().length > 0;
            evidenzia(0);
        };
        const apri = () => { input.value = ''; filtra(); dlg.showModal(); input.focus(); };

        $$('[data-palette-open]').forEach(b => b.addEventListener('click', apri));
        document.addEventListener('keydown', e => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); dlg.open ? dlg.close() : apri(); }
        });
        input.addEventListener('input', filtra);
        input.addEventListener('keydown', e => {
            if (e.key === 'ArrowDown') { e.preventDefault(); evidenzia(attiva + 1); }
            if (e.key === 'ArrowUp') { e.preventDefault(); evidenzia(attiva - 1); }
            if (e.key === 'Enter') { e.preventDefault(); visibili()[attiva]?.click(); dlg.close(); }
        });
        dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });
    }

    function flash() {
        $$('[data-flash]').forEach(el => Lume.toast(el.textContent.trim(), el.dataset.flash));
    }

    /* =================================================================
       Comportamenti generici
       ================================================================= */

    /** Chiede conferma prima di inviare: <form data-conferma="Sei sicuro?"> */
    function conferme() {
        document.addEventListener('submit', e => {
            const form = e.target;
            const domanda = e.submitter?.dataset.conferma || form.dataset.conferma;
            if (domanda && !form.dataset.confermato) {
                e.preventDefault();
                e.stopImmediatePropagation();
                chiediConferma(domanda, e.submitter?.dataset.confermaOk).then(ok => {
                    if (!ok) return;
                    form.dataset.confermato = '1';
                    form.requestSubmit(e.submitter);
                    delete form.dataset.confermato;
                });
            }
        }, true);
    }

    function chiediConferma(testo, etichettaOk = 'Conferma') {
        return new Promise(risolvi => {
            const d = document.createElement('dialog');
            d.className = 'modal modal--conferma';
            d.innerHTML = `
                <div class="modal__body">
                    <span class="empty__icon" style="background:var(--danger-soft);color:var(--danger)">${icona('avviso')}</span>
                    <p style="font-size:1rem;font-weight:500"></p>
                </div>
                <div class="modal__foot">
                    <button type="button" class="btn btn--ghost" value="no">Annulla</button>
                    <button type="button" class="btn btn--primary" value="si"></button>
                </div>`;
            d.querySelector('p').textContent = testo;
            d.querySelector('[value=si]').textContent = etichettaOk;
            document.body.append(d);
            const fine = ok => { d.close(); d.remove(); risolvi(ok); };
            d.querySelector('[value=no]').onclick = () => fine(false);
            d.querySelector('[value=si]').onclick = () => fine(true);
            d.addEventListener('cancel', e => { e.preventDefault(); fine(false); });
            d.showModal();
            d.querySelector('[value=si]').focus();
        });
    }
    Lume.conferma = chiediConferma;

    /**
     * Moduli che si inviano senza ricaricare la pagina: <form data-ajax>.
     * Il server risponde con la pagina aggiornata (dopo il redirect): sostituiamo
     * solo le zone marcate con data-swap="nome" e mostriamo i messaggi.
     */
    function ajax() {
        document.addEventListener('submit', async e => {
            const form = e.target.closest('form[data-ajax]');
            if (!form || e.defaultPrevented) return;
            e.preventDefault();
            const zona = form.closest('[data-swap]') || form;
            zona.classList.add('is-busy');
            try {
                const dati = new FormData(form);
                if (e.submitter?.name) dati.append(e.submitter.name, e.submitter.value);
                const r = await fetch(form.action, {
                    method: 'POST',
                    body: new URLSearchParams(dati),
                    headers: { Accept: 'text/html', 'X-Requested-With': 'fetch' }
                });
                if (r.redirected && !r.url.startsWith(location.href.split('#')[0].split('?')[0])) {
                    location.href = r.url;
                    return;
                }
                const html = await r.text();
                const doc = new DOMParser().parseFromString(html, 'text/html');
                aggiorna(doc);
                $$('[data-flash]', doc).forEach(el => Lume.toast(el.textContent.trim(), el.dataset.flash));
            } catch (err) {
                Lume.toast('Connessione non riuscita, riprova', 'errore');
            } finally {
                zona.classList.remove('is-busy');
            }
        });
    }

    /** Sostituisce le zone data-swap della pagina con quelle di un nuovo documento. */
    function aggiorna(doc) {
        $$('[data-swap]', doc).forEach(nuovo => {
            const vecchio = $(`[data-swap="${nuovo.dataset.swap}"]`);
            if (!vecchio) return;
            const aperti = $$('details[open]', vecchio).map(d => d.dataset.id).filter(Boolean);
            vecchio.replaceWith(nuovo);
            aperti.forEach(id => { const d = $(`details[data-id="${id}"]`, nuovo); if (d) d.open = true; });
            attiva(nuovo);
            document.dispatchEvent(new CustomEvent('lume:aggiornato', { detail: nuovo }));
        });
    }
    Lume.aggiorna = aggiorna;

    /** Scarica di nuovo la pagina corrente e aggiorna solo le zone data-swap. */
    Lume.ricarica = async () => {
        const r = await fetch(location.href, { headers: { Accept: 'text/html' } });
        if (r.status === 401 || r.redirected) { location.reload(); return; }
        aggiorna(new DOMParser().parseFromString(await r.text(), 'text/html'));
    };

    /** Finestre modali: <button data-modal-open="id"> apre <dialog id="id">. */
    function modali() {
        document.addEventListener('click', e => {
            const apri = e.target.closest('[data-modal-open]');
            if (apri) {
                const d = document.getElementById(apri.dataset.modalOpen);
                if (d) { d.showModal(); d.dispatchEvent(new CustomEvent('lume:apri', { detail: apri })); }
            }
            const chiudi = e.target.closest('[data-modal-close]');
            if (chiudi) chiudi.closest('dialog')?.close();
            if (e.target.matches('dialog.modal')) e.target.close();
        });
    }

    /** Campi numerici con − e +: <div class="stepper" data-stepper> */
    function stepper(root = document) {
        $$('[data-stepper]', root).forEach(s => {
            if (s.dataset.pronto) return;
            s.dataset.pronto = '1';
            const input = $('input', s);
            $$('button[data-delta]', s).forEach(b => b.addEventListener('click', () => {
                const min = Number(input.min || 0), max = Number(input.max || 999);
                input.value = Math.min(max, Math.max(min, Number(input.value || 0) + Number(b.dataset.delta)));
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }));
        });
    }

    /** Invia il form appena cambia un campo: <select data-auto-submit> */
    function autoInvio() {
        document.addEventListener('change', e => {
            if (e.target.matches('[data-auto-submit]')) e.target.form?.requestSubmit();
        });
    }

    /** Filtro testuale di righe: <input data-filtra="#tabella tbody tr"> */
    function filtri(root = document) {
        $$('[data-filtra]', root).forEach(input => {
            const righe = () => $$(input.dataset.filtra);
            input.addEventListener('input', () => {
                const q = input.value.trim().toLowerCase();
                righe().forEach(r => { r.hidden = q && !r.textContent.toLowerCase().includes(q); });
            });
        });
    }

    /** Ricarica la pagina ogni N secondi se l'utente non sta facendo nulla: <main data-refresh="60"> */
    function autoRefresh() {
        const el = $('[data-refresh]');
        if (!el) return;
        let ultimo = Date.now();
        ['pointerdown', 'keydown', 'scroll'].forEach(ev => addEventListener(ev, () => { ultimo = Date.now(); }, { passive: true }));
        setInterval(async () => {
            if (document.hidden || Date.now() - ultimo < 15000 || $('dialog[open]')) return;
            try {
                const r = await fetch(location.href, { headers: { Accept: 'text/html' } });
                if (!r.ok || r.redirected) return;
                const doc = new DOMParser().parseFromString(await r.text(), 'text/html');
                const nuovo = $('[data-refresh]', doc);
                if (nuovo) { el.innerHTML = nuovo.innerHTML; attiva(el); }
            } catch (e) { /* riproviamo al prossimo giro */ }
        }, Number(el.dataset.refresh) * 1000);
    }

    /* =================================================================
       Grafici SVG
       ================================================================= */

    const NS = 'http://www.w3.org/2000/svg';
    const PALETTE = ['#7A1E35', '#B08A3E', '#3557A6', '#2F7D5B', '#6A4BA6', '#B26B05', '#978C83'];

    function tooltip(box) {
        let tip = $('.chart-tip', box);
        if (!tip) { tip = document.createElement('div'); tip.className = 'chart-tip'; box.append(tip); }
        return {
            show(x, y, titolo, valore) {
                tip.innerHTML = `<strong>${Lume.escape(valore)}</strong>${Lume.escape(titolo)}`;
                tip.style.left = x + 'px';
                tip.style.top = y + 'px';
                tip.classList.add('is-on');
            },
            hide() { tip.classList.remove('is-on'); }
        };
    }

    function scalaMassimo(max) {
        if (max <= 0) return 1;
        const p = Math.pow(10, Math.floor(Math.log10(max)));
        const n = max / p;
        return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10) * p;
    }

    function graficoBarre(box, punti) {
        const W = box.clientWidth || 600, H = Number(box.dataset.altezza) || 220;
        const pad = { t: 12, r: 8, b: 26, l: box.dataset.euro !== undefined ? 52 : 34 };
        const valori = punti.map(p => Number(p.valore) || 0);
        const max = scalaMassimo(Math.max(...valori, 0));
        const fmt = box.dataset.euro !== undefined ? Lume.euroTondo : v => numero.format(v);
        const iw = W - pad.l - pad.r, ih = H - pad.t - pad.b;
        const passo = iw / Math.max(punti.length, 1);
        const larg = Math.min(38, passo * 0.62);
        const ogniN = Math.ceil(punti.length / Math.max(1, Math.floor(iw / 46)));

        let svg = `<svg viewBox="0 0 ${W} ${H}" role="img">`;
        for (let i = 0; i <= 4; i++) {
            const y = pad.t + ih - ih * i / 4;
            svg += `<line class="ax" x1="${pad.l}" x2="${W - pad.r}" y1="${y}" y2="${y}" ${i ? 'stroke-dasharray="3 4"' : ''}/>`;
            svg += `<text class="ax-label" x="${pad.l - 8}" y="${y + 4}" text-anchor="end">${fmt(max * i / 4)}</text>`;
        }
        punti.forEach((p, i) => {
            const v = valori[i];
            const h = Math.max(v > 0 ? 2 : 0, ih * v / max);
            const x = pad.l + passo * i + (passo - larg) / 2;
            const ultimo = box.dataset.evidenziaUltimo !== undefined && i === punti.length - 1;
            svg += `<rect class="bar${ultimo ? ' bar--alt' : ''}" x="${x}" y="${pad.t + ih - h}" width="${larg}" height="${h}" rx="${Math.min(5, larg / 4)}" data-i="${i}"/>`;
            if (i % ogniN === 0) {
                svg += `<text class="ax-label" x="${x + larg / 2}" y="${H - 6}" text-anchor="middle">${Lume.escape(p.etichetta)}</text>`;
            }
            svg += `<rect class="hit" x="${pad.l + passo * i}" y="${pad.t}" width="${passo}" height="${ih}" data-i="${i}"/>`;
        });
        svg += '</svg>';
        box.innerHTML = svg;

        const tip = tooltip(box);
        $$('.hit', box).forEach(r => {
            r.addEventListener('pointerenter', () => {
                const i = Number(r.dataset.i);
                const bar = $(`.bar[data-i="${i}"]`, box);
                bar.classList.add('is-hover');
                const scala = box.clientWidth / W;
                tip.show((Number(bar.getAttribute('x')) + larg / 2) * scala, Number(bar.getAttribute('y')) * scala, punti[i].etichetta, fmt(valori[i]));
            });
            r.addEventListener('pointerleave', () => { $$('.bar', box).forEach(b => b.classList.remove('is-hover')); tip.hide(); });
        });
    }

    function graficoLinea(box, punti) {
        const W = box.clientWidth || 600, H = Number(box.dataset.altezza) || 220;
        const pad = { t: 14, r: 12, b: 26, l: box.dataset.euro !== undefined ? 52 : 34 };
        const valori = punti.map(p => Number(p.valore) || 0);
        const max = scalaMassimo(Math.max(...valori, 0));
        const fmt = box.dataset.euro !== undefined ? Lume.euroTondo : v => numero.format(v);
        const iw = W - pad.l - pad.r, ih = H - pad.t - pad.b;
        const x = i => pad.l + (punti.length < 2 ? iw / 2 : iw * i / (punti.length - 1));
        const y = v => pad.t + ih - ih * v / max;
        const ogniN = Math.ceil(punti.length / Math.max(1, Math.floor(iw / 50)));

        // curva morbida (Catmull-Rom → Bézier)
        const pts = valori.map((v, i) => [x(i), y(v)]);
        let d = pts.length ? `M${pts[0][0]},${pts[0][1]}` : '';
        for (let i = 0; i < pts.length - 1; i++) {
            const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || p2;
            const c1 = [p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6];
            const c2 = [p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6];
            d += ` C${c1[0]},${Math.min(c1[1], pad.t + ih)} ${c2[0]},${Math.min(c2[1], pad.t + ih)} ${p2[0]},${p2[1]}`;
        }
        const id = 'g' + Math.random().toString(36).slice(2, 8);
        let svg = `<svg viewBox="0 0 ${W} ${H}" role="img"><defs><linearGradient id="${id}" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0" stop-color="var(--wine)" stop-opacity=".22"/><stop offset="1" stop-color="var(--wine)" stop-opacity="0"/></linearGradient></defs>`;
        for (let i = 0; i <= 4; i++) {
            const yy = pad.t + ih - ih * i / 4;
            svg += `<line class="ax" x1="${pad.l}" x2="${W - pad.r}" y1="${yy}" y2="${yy}" ${i ? 'stroke-dasharray="3 4"' : ''}/>`;
            svg += `<text class="ax-label" x="${pad.l - 8}" y="${yy + 4}" text-anchor="end">${fmt(max * i / 4)}</text>`;
        }
        if (pts.length) {
            svg += `<path d="${d} L${pts[pts.length - 1][0]},${pad.t + ih} L${pts[0][0]},${pad.t + ih} Z" fill="url(#${id})"/>`;
            svg += `<path class="line" d="${d}"/>`;
        }
        punti.forEach((p, i) => {
            if (i % ogniN === 0 || i === punti.length - 1) {
                svg += `<text class="ax-label" x="${x(i)}" y="${H - 6}" text-anchor="middle">${Lume.escape(p.etichetta)}</text>`;
            }
        });
        svg += `<line class="ax guida" x1="0" x2="0" y1="${pad.t}" y2="${pad.t + ih}" stroke-dasharray="3 3" style="display:none"/>`;
        svg += `<circle class="pt" r="5" style="display:none"/>`;
        svg += `<rect class="hit" x="${pad.l}" y="${pad.t}" width="${iw}" height="${ih}"/></svg>`;
        box.innerHTML = svg;

        const tip = tooltip(box), hit = $('.hit', box), pt = $('.pt', box), guida = $('.guida', box);
        hit.addEventListener('pointermove', e => {
            const r = box.getBoundingClientRect();
            const scala = r.width / W;
            const mx = (e.clientX - r.left) / scala;
            const i = Math.max(0, Math.min(punti.length - 1, Math.round((mx - pad.l) / (iw / Math.max(1, punti.length - 1)))));
            pt.setAttribute('cx', pts[i][0]); pt.setAttribute('cy', pts[i][1]); pt.style.display = '';
            guida.setAttribute('x1', pts[i][0]); guida.setAttribute('x2', pts[i][0]); guida.style.display = '';
            tip.show(pts[i][0] * scala, pts[i][1] * scala, punti[i].etichetta, fmt(valori[i]));
        });
        hit.addEventListener('pointerleave', () => { pt.style.display = 'none'; guida.style.display = 'none'; tip.hide(); });
    }

    function graficoCiambella(box, punti) {
        const tot = punti.reduce((s, p) => s + (Number(p.valore) || 0), 0) || 1;
        const R = 60, C = 2 * Math.PI * R;
        let off = 0;
        const fmt = box.dataset.euro !== undefined ? Lume.euroTondo : v => numero.format(v);
        let archi = '';
        punti.forEach((p, i) => {
            const frac = (Number(p.valore) || 0) / tot;
            const len = Math.max(0, frac * C - 2);
            archi += `<circle cx="75" cy="75" r="${R}" fill="none" stroke="${PALETTE[i % PALETTE.length]}" stroke-width="22"
                stroke-dasharray="${len} ${C - len}" stroke-dashoffset="${-off}" transform="rotate(-90 75 75)"><title>${Lume.escape(p.etichetta)}</title></circle>`;
            off += frac * C;
        });
        box.classList.add('donut');
        box.innerHTML = `<svg viewBox="0 0 150 150">${archi}
            <text x="75" y="72" text-anchor="middle" style="font:500 ${fmt(box.dataset.totale ?? tot).length > 7 ? 15 : 20}px var(--display);fill:var(--text)">${fmt(box.dataset.totale ?? tot)}</text>
            <text x="75" y="92" text-anchor="middle" style="font:600 10px var(--testo);fill:var(--text-3);letter-spacing:.1em">${Lume.escape(box.dataset.etichetta || 'TOTALE')}</text></svg>
            <ul class="donut__list">${punti.map((p, i) => `<li><i style="background:${PALETTE[i % PALETTE.length]}"></i>
                <span>${Lume.escape(p.etichetta)}</span><b>${Math.round((Number(p.valore) || 0) * 100 / tot)}%</b></li>`).join('')}</ul>`;
    }

    function grafici(root = document) {
        $$('[data-chart]', root).forEach(box => {
            let punti = box._punti || Lume.dati(box) || [];
            // una mappa {"Carta": 12} diventa [{etichetta: "Carta", valore: 12}]
            if (!Array.isArray(punti)) punti = Object.entries(punti).map(([etichetta, valore]) => ({ etichetta, valore }));
            box._punti = punti;
            const disegna = () => {
                if (!punti.length) { box.innerHTML = '<div class="empty"><small>Nessun dato nel periodo</small></div>'; return; }
                ({ barre: graficoBarre, linea: graficoLinea, ciambella: graficoCiambella }[box.dataset.chart] || graficoBarre)(box, punti);
            };
            disegna();
            if (!box._osservato && box.dataset.chart !== 'ciambella') {
                box._osservato = true;
                let w = box.clientWidth;
                new ResizeObserver(() => { if (Math.abs(box.clientWidth - w) > 4) { w = box.clientWidth; disegna(); } }).observe(box);
            }
        });
    }

    /* =================================================================
       Attivazione
       ================================================================= */

    const moduli = [];
    Lume.modulo = fn => moduli.push(fn);

    /** Attiva i comportamenti su una zona della pagina (anche dopo una sostituzione AJAX). */
    function attiva(root) {
        stepper(root);
        filtri(root);
        grafici(root);
        moduli.forEach(fn => fn(root));
    }

    /* =================================================================
       Pagine
       ================================================================= */

    /* ---------- Sala: mappa interattiva, aggiornamento automatico, spostamento tavoli ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-sala]');
        if (!pagina) return;

        const parametri = new URLSearchParams(location.search);
        let scelto = parametri.get('tavolo');
        let firma = null;
        let modifica = false;

        const seleziona = id => {
            scelto = id ? String(id) : null;
            $$('.tavolo', pagina).forEach(t => t.classList.toggle('is-selected', t.dataset.id === scelto));
            const pannelli = $$('[data-pannello]', pagina);
            const pannello = pannelli.find(p => p.dataset.pannello === scelto);
            pannelli.forEach(p => { p.hidden = p !== (pannello || pannelli.find(x => x.dataset.pannello === '0')); });
            const url = new URL(location.href);
            scelto ? url.searchParams.set('tavolo', scelto) : url.searchParams.delete('tavolo');
            history.replaceState(null, '', url);
            if (pannello && innerWidth < 1240) pannello.scrollIntoView({ behavior: 'smooth', block: 'start' });
        };

        pagina.addEventListener('click', e => {
            const t = e.target.closest('.tavolo');
            if (t && !modifica) seleziona(t.dataset.id === scelto ? null : t.dataset.id);
            const s = e.target.closest('[data-scegli]');
            if (s) seleziona(s.dataset.scegli);
        });
        document.addEventListener('keydown', e => { if (e.key === 'Escape' && !$('dialog[open]')) seleziona(null); });
        document.addEventListener('lume:aggiornato', () => seleziona(scelto));
        seleziona(scelto);

        // aggiornamento automatico: se qualcosa cambia ricarichiamo solo la mappa
        const controlla = async () => {
            if (document.hidden || modifica || pagina.contains(document.activeElement) && document.activeElement.matches('input')) return;
            try {
                const stato = await Lume.get(pagina.dataset.api);
                const nuova = JSON.stringify(stato.map(t => [t.id, t.stato, t.minuti, t.totale, t.coperti, t.prenotazione]));
                if (firma && nuova !== firma) {
                    const prima = Object.fromEntries($$('.tavolo', pagina).map(t => [t.dataset.id, t.dataset.stato]));
                    await Lume.ricarica();
                    $$('.tavolo', pagina).forEach(t => {
                        if (prima[t.dataset.id] && prima[t.dataset.id] !== t.dataset.stato) {
                            t.classList.add('is-cambiato');
                            if (t.dataset.stato === 'pronto') Lume.toast(`Tavolo ${$('strong', t).textContent}: piatti pronti in cucina`);
                        }
                    });
                }
                firma = nuova;
            } catch (e) { /* rete assente: riproviamo */ }
        };
        controlla();
        setInterval(controlla, 8000);

        // spostamento dei tavoli (solo amministratore)
        const bottone = $('[data-sala-modifica]', pagina);
        if (!bottone) return;
        bottone.addEventListener('click', () => {
            modifica = !modifica;
            $('[data-mappa]', pagina).classList.toggle('is-editing', modifica);
            $('[data-sala-hint]', pagina).hidden = !modifica;
            bottone.classList.toggle('btn--primary', modifica);
            $('span', bottone).textContent = modifica ? 'Fatto' : 'Sposta i tavoli';
            if (modifica) seleziona(null);
        });

        let trascina = null;
        pagina.addEventListener('pointerdown', e => {
            const t = e.target.closest('.tavolo');
            if (!modifica || !t) return;
            e.preventDefault();
            t.setPointerCapture(e.pointerId);
            t.classList.add('is-dragging');
            trascina = { t, mappa: t.parentElement.getBoundingClientRect() };
        });
        pagina.addEventListener('pointermove', e => {
            if (!trascina) return;
            const { t, mappa } = trascina;
            const x = Math.max(3, Math.min(97, (e.clientX - mappa.left) / mappa.width * 100));
            const y = Math.max(5, Math.min(95, (e.clientY - mappa.top) / mappa.height * 100));
            t.style.left = x + '%';
            t.style.top = y + '%';
            trascina.x = Math.round(x);
            trascina.y = Math.round(y);
        });
        const lascia = async () => {
            if (!trascina) return;
            const { t, x, y } = trascina;
            trascina = null;
            t.classList.remove('is-dragging');
            if (x == null) return;
            try {
                await Lume.post(`${pagina.dataset.sposta}${t.dataset.id}/posizione`, { x, y });
                Lume.toast(`Tavolo ${$('strong', t).textContent} spostato`);
            } catch (err) { Lume.toast(err.message, 'errore'); }
        };
        pagina.addEventListener('pointerup', lascia);
        pagina.addEventListener('pointercancel', lascia);
    });

    /* ---------- Comanda: scelta piatti, aggiunta con nota, chiusura del conto ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-comanda]');
        if (!pagina) return;

        // ricerca e categorie
        const scelta = $('[data-scelta]', pagina);
        if (scelta) {
            const cerca = $('[data-scelta-cerca]', scelta);
            const schede = $$('[data-cat]', $('.scelta__tabs', scelta));
            const piatti = $$('.piatto-btn-form', scelta);
            let categoria = 'tutti';
            const filtra = () => {
                const q = cerca.value.trim().toLowerCase();
                let n = 0;
                piatti.forEach(f => {
                    const ok = (categoria === 'tutti' || f.dataset.cat === categoria) && (!q || f.dataset.nome.includes(q));
                    f.hidden = !ok;
                    if (ok) n++;
                });
                $('[data-scelta-vuoto]', scelta).hidden = n > 0;
            };
            schede.forEach(b => b.addEventListener('click', () => {
                categoria = b.dataset.cat;
                schede.forEach(x => x.classList.toggle('is-active', x === b));
                filtra();
            }));
            cerca.addEventListener('input', filtra);
            cerca.addEventListener('keydown', e => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const primo = piatti.find(f => !f.hidden && !$('button[type=submit]', f).disabled);
                    if (primo) { primo.requestSubmit(); cerca.select(); }
                }
            });
            // piccolo segnale visivo sul piatto appena aggiunto
            scelta.addEventListener('submit', e => {
                const f = e.target.closest('.piatto-btn-form');
                if (!f) return;
                f.classList.remove('is-aggiunto');
                void f.offsetWidth;
                f.classList.add('is-aggiunto');
            });
        }

        // aggiunta con nota
        const modaleNota = $('[data-modale-nota]');
        if (modaleNota) {
            const form = $('form', modaleNota);
            modaleNota.addEventListener('lume:apri', e => {
                form.reset();
                form.piattoId.value = e.detail.dataset.piatto;
                $('[data-nota-titolo]', modaleNota).textContent = e.detail.dataset.nome;
                setTimeout(() => $('[data-nota-input]', modaleNota).focus(), 50);
            });
            $$('[data-nota-chips] .chip', modaleNota).forEach(c => c.addEventListener('click', () => {
                const input = $('[data-nota-input]', modaleNota);
                input.value = input.value ? `${input.value}, ${c.textContent}` : c.textContent;
                input.focus();
            }));
            form.addEventListener('submit', () => setTimeout(() => modaleNota.close(), 0));
        }

        // chiusura del conto: riepilogo, sconto e resto calcolati dal vivo
        const modaleConto = $('[data-modale-conto]');
        if (modaleConto) {
            const form = $('form', modaleConto);
            const calcola = () => {
                const c = $('[data-swap="comanda"]');
                const base = Number(c.dataset.subtotale) + Number(c.dataset.copertiImporto);
                const sconto = Number(form.querySelector('[name=sconto]:checked')?.value || 0);
                const importoSconto = Math.round(base * sconto) / 100;
                const totale = base - importoSconto;
                $('[data-conto-base]', modaleConto).textContent = Lume.euro(base);
                $('[data-conto-riga-sconto]', modaleConto).hidden = !sconto;
                $('[data-conto-sconto]', modaleConto).textContent = '− ' + Lume.euro(importoSconto);
                $('[data-conto-totale]', modaleConto).textContent = Lume.euro(totale);
                const contanti = form.querySelector('[name=metodo]:checked')?.value === 'CONTANTI';
                $('[data-resto]', modaleConto).hidden = !contanti;
                const ricevuti = Number($('[data-resto-input]', modaleConto).value.replace(',', '.'));
                const resto = ricevuti - totale;
                $('[data-resto-valore]', modaleConto).textContent = ricevuti ? (resto >= 0 ? Lume.euro(resto) : 'mancano ' + Lume.euro(-resto)) : '—';
                $('[data-resto-valore]', modaleConto).style.color = resto < 0 ? 'var(--danger)' : '';
            };
            modaleConto.addEventListener('lume:apri', calcola);
            form.addEventListener('input', calcola);
            form.addEventListener('change', calcola);
        }
    });

    /* ---------- Monitor della cucina ---------- */
    Lume.modulo(root => {
        const kds = root === document && $('[data-kds]');
        if (!kds) return;

        const griglia = $('[data-kds-griglia]', kds);
        const api = kds.dataset.api;
        let tickets = Lume.dati(griglia) || [];
        let conosciuti = new Set(tickets.map(t => t.ordineId));
        let suono = true;
        try { suono = localStorage.getItem('lume-kds-suono') !== 'off'; } catch (e) { /* niente */ }

        const btnSuono = $('[data-kds-suono]', kds);
        btnSuono.setAttribute('aria-pressed', String(suono));
        btnSuono.addEventListener('click', () => {
            suono = !suono;
            btnSuono.setAttribute('aria-pressed', String(suono));
            try { localStorage.setItem('lume-kds-suono', suono ? 'on' : 'off'); } catch (e) { /* niente */ }
            if (suono) campanello();
        });
        $('[data-kds-schermo]', kds).addEventListener('click', () => {
            document.fullscreenElement ? document.exitFullscreen() : document.documentElement.requestFullscreen?.();
        });

        // "din-don" generato al momento, senza file audio
        let audio;
        function campanello() {
            if (!suono) return;
            try {
                audio = audio || new (window.AudioContext || window.webkitAudioContext)();
                [[880, 0], [660, .18]].forEach(([f, t]) => {
                    const o = audio.createOscillator(), g = audio.createGain();
                    o.type = 'sine';
                    o.frequency.value = f;
                    g.gain.setValueAtTime(0.0001, audio.currentTime + t);
                    g.gain.exponentialRampToValueAtTime(0.35, audio.currentTime + t + 0.02);
                    g.gain.exponentialRampToValueAtTime(0.0001, audio.currentTime + t + 0.6);
                    o.connect(g).connect(audio.destination);
                    o.start(audio.currentTime + t);
                    o.stop(audio.currentTime + t + 0.65);
                });
            } catch (e) { /* audio non disponibile */ }
        }

        const timer = m => m < 60 ? `${m}′` : `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}′`;
        const etichetta = { INVIATO: 'in coda', IN_PREPARAZIONE: 'sul fuoco', PRONTO: 'pronto' };
        const urgenze = { regolare: 'in orario', attenzione: 'attenzione', ritardo: 'in ritardo', pronto: 'pronto' };

        function disegna(nuovi = new Set()) {
            const conta = { INVIATO: 0, IN_PREPARAZIONE: 0, PRONTO: 0 };
            tickets.forEach(t => t.righe.forEach(r => { conta[r.stato] = (conta[r.stato] || 0) + r.quantita; }));
            Object.entries(conta).forEach(([k, v]) => { const el = $(`[data-conta="${k}"]`, kds); if (el) el.textContent = v; });
            $('[data-kds-vuoto]', kds).hidden = tickets.length > 0;

            griglia.innerHTML = tickets.map(t => {
                const uscite = [...new Set(t.righe.map(r => r.uscita))];
                const righe = uscite.map(u => `<p class="ticket__uscita">${u}ª uscita</p><ul class="ticket__righe">${
                    t.righe.filter(r => r.uscita === u).map(r => `<li class="kpiatto is-${r.stato}">
                        <button type="button" class="kpiatto__btn" data-avanti="${r.id}" ${r.stato === 'PRONTO' ? 'disabled' : ''}>
                            <span class="kpiatto__q">${r.quantita}</span>
                            <span><span class="kpiatto__nome">${Lume.escape(r.piatto)}</span>
                                ${r.note ? `<span class="kpiatto__nota">${Lume.escape(r.note)}</span>` : ''}</span>
                            <span class="kpiatto__stato">${etichetta[r.stato]}</span>
                        </button>
                        <button type="button" class="kpiatto__indietro" data-indietro="${r.id}" title="Annulla l'ultimo tocco" aria-label="Indietro">↺</button>
                    </li>`).join('')}</ul>`).join('');
                return `<article class="ticket is-${t.urgenza}${nuovi.has(t.ordineId) ? ' is-nuovo' : ''}" data-ordine="${t.ordineId}">
                    <header class="ticket__head">
                        <div>
                            <p class="ticket__tavolo">${t.tavolo}<small>${t.coperti} coperti</small></p>
                            <p class="ticket__chi">${Lume.escape(t.cameriere)}</p>
                        </div>
                        <div class="ticket__timer"><strong>${timer(t.minutiAttesa)}</strong><small>${urgenze[t.urgenza]}</small></div>
                    </header>
                    ${righe}
                    <footer class="ticket__foot">
                        <button type="button" class="ticket__tutto" data-tutto="${t.ordineId}">
                            ${t.urgenza === 'pronto' ? '✓ In attesa della sala' : 'Tutto pronto'}</button>
                    </footer>
                </article>`;
            }).join('');
        }

        async function azione(url, card) {
            card?.classList.add('is-busy');
            try {
                const json = await Lume.post(url);
                tickets = json.tickets;
                disegna();
            } catch (e) {
                Lume.toast(e.message, 'errore');
                card?.classList.remove('is-busy');
            }
        }

        griglia.addEventListener('click', e => {
            const card = e.target.closest('.ticket');
            if (!card) return;
            const id = card.dataset.ordine;
            const avanti = e.target.closest('[data-avanti]');
            const indietro = e.target.closest('[data-indietro]');
            const tutto = e.target.closest('[data-tutto]');
            if (avanti) azione(`${api}/${id}/righe/${avanti.dataset.avanti}/avanti`, card);
            if (indietro) azione(`${api}/${id}/righe/${indietro.dataset.indietro}/indietro`, card);
            if (tutto && !card.classList.contains('is-pronto')) azione(`${api}/${id}/pronto`, card);
        });

        async function aggiorna() {
            if (document.hidden) return;
            try {
                const json = await Lume.get(api);
                const nuovi = new Set(json.tickets.map(t => t.ordineId).filter(id => !conosciuti.has(id)));
                // anche nuovi piatti inviati per un tavolo già presente contano come novità
                json.tickets.forEach(t => {
                    const prima = tickets.find(x => x.ordineId === t.ordineId);
                    if (prima && t.righe.some(r => !prima.righe.some(p => p.id === r.id))) nuovi.add(t.ordineId);
                });
                const cambiato = JSON.stringify(json.tickets) !== JSON.stringify(tickets);
                tickets = json.tickets;
                conosciuti = new Set(tickets.map(t => t.ordineId));
                if (cambiato) disegna(nuovi);
                if (nuovi.size) campanello();
            } catch (e) { /* riproviamo tra poco */ }
        }

        disegna();
        setInterval(aggiorna, 5000);
    });

    /* ---------- Menù: disponibilità al volo e modale categoria ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-menu-admin]');
        if (!pagina) return;

        pagina.addEventListener('change', async e => {
            const form = e.target.closest('form[data-disponibilita]');
            if (!form) return;
            const riga = form.closest('tr');
            try {
                const r = await fetch(form.action, {
                    method: 'POST',
                    headers: { Accept: 'application/json', ...Lume.csrf() },
                    body: new URLSearchParams(new FormData(form))
                });
                if (!r.ok) throw new Error();
                const json = await r.json();
                riga.classList.toggle('is-off', !json.disponibile);
                e.target.checked = json.disponibile;
                Lume.toast(json.nome + (json.disponibile ? ' di nuovo disponibile' : ' segnato come esaurito'));
            } catch (err) {
                e.target.checked = !e.target.checked;
                Lume.toast('Non sono riuscito a salvare, riprova', 'errore');
            }
        });

        const modale = $('[data-modale-categoria]');
        modale?.addEventListener('lume:apri', e => {
            const d = e.detail.dataset;
            const form = $('form', modale);
            const nuova = !d.id;
            form.action = nuova ? modale.dataset.nuova : d.azione;
            form.nome.value = nuova ? '' : d.nome;
            form.sottotitolo.value = nuova ? '' : (d.sottotitolo || '');
            const uscita = form.querySelector(`[name=uscita][value="${nuova ? 1 : d.uscita}"]`);
            if (uscita) uscita.checked = true;
            $('[data-titolo]', modale).textContent = nuova ? 'Nuova categoria' : 'Modifica categoria';
            setTimeout(() => form.nome.focus(), 50);
        });
    });

    /* ---------- Scheda piatto: ricetta dinamica, food cost dal vivo, anteprima, testi con l'AI ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-piatto-form]');
        if (!pagina) return;

        const form = $('[data-ricetta-form]', pagina);
        const ricetta = $('[data-ricetta]', pagina);
        const modello = $('[data-riga-modello]', pagina);
        const prezzo = $('[data-prezzo]', pagina);

        const rinumera = () => $$('[data-riga]', ricetta).forEach((r, i) => {
            $('[data-ingr]', r).name = `ricetta[${i}].ingredienteId`;
            $('[data-qta]', r).name = `ricetta[${i}].quantita`;
        });

        const calcola = () => {
            let costo = 0;
            $$('[data-riga]', ricetta).forEach(r => {
                const opt = $('[data-ingr]', r).selectedOptions[0];
                const q = Number($('[data-qta]', r).value) || 0;
                const c = opt?.dataset.costo ? Number(opt.dataset.costo) * q : 0;
                $('[data-unita]', r).textContent = opt?.dataset.unita || '—';
                $('[data-costo-riga]', r).textContent = Lume.euro(c);
                costo += c;
            });
            const p = Number(prezzo.value) || 0;
            const perc = p > 0 ? Math.round(costo * 100 / p) : 0;
            const fc = $('[data-foodcost]', pagina);
            $('[data-fc-costo]', fc).textContent = Lume.euro(costo);
            $('[data-fc-prezzo]', fc).textContent = Lume.euro(p);
            $('[data-fc-margine]', fc).textContent = Lume.euro(p - costo);
            $('[data-fc-perc]', fc).textContent = costo > 0 && p > 0 ? perc + '%' : '—';
            const gauge = $('[data-gauge]', fc);
            gauge.style.strokeDasharray = `${Math.min(100, perc * 100 / 60)} 100`;
            gauge.style.stroke = perc < 30 ? 'var(--ok)' : perc <= 38 ? 'var(--warn)' : 'var(--danger)';
            $('[data-fc-nota]', fc).textContent = costo === 0
                ? 'Aggiungi gli ingredienti per calcolare il food cost.'
                : perc < 30 ? 'Ottimo: nella ristorazione di qualità si punta a stare sotto il 30%.'
                : perc <= 38 ? 'Nella media: valuta porzioni o prezzo.'
                : 'Food cost alto: il piatto rende poco, rivedi prezzo o ricetta.';
            $('[data-ant="prezzo"]', pagina).textContent = p ? Lume.euro(p).replace(',00', '') : '—';
        };

        const anteprima = () => {
            $$('[data-anteprima]', pagina).forEach(el => {
                const t = $(`[data-ant="${el.dataset.anteprima}"]`, pagina);
                const vuoto = { nome: 'Nome del piatto', descrizione: 'La descrizione comparirà qui.', abbinamento: '' }[el.dataset.anteprima];
                t.textContent = el.value.trim() ? (el.dataset.anteprima === 'abbinamento' ? '🍷 ' : '') + el.value.trim() : vuoto;
            });
            const desc = $('[data-anteprima="descrizione"]', pagina);
            const conta = $('[data-conta-caratteri]', pagina);
            if (conta) conta.textContent = `${desc.value.length}/${conta.dataset.contaCaratteri}`;
        };

        $('[data-aggiungi-riga]', pagina).addEventListener('click', () => {
            const riga = modello.content.firstElementChild.cloneNode(true);
            modello.before(riga);
            rinumera();
            $('select', riga).focus();
        });
        ricetta.addEventListener('click', e => {
            const togli = e.target.closest('[data-togli]');
            if (!togli) return;
            togli.closest('[data-riga]').remove();
            rinumera();
            calcola();
        });
        form.addEventListener('input', () => { calcola(); anteprima(); });
        form.addEventListener('change', calcola);
        form.addEventListener('submit', () => {
            // le righe incomplete non vengono inviate
            $$('[data-riga]', ricetta).forEach(r => {
                if (!$('[data-ingr]', r).value || !(Number($('[data-qta]', r).value) > 0)) r.remove();
            });
            rinumera();
        });
        if (!$$('[data-riga]', ricetta).length) $('[data-aggiungi-riga]', pagina).click();
        rinumera();
        calcola();
        anteprima();

        // testi scritti dall'AI, con effetto "macchina da scrivere"
        const scrivi = (campo, testo) => new Promise(fine => {
            campo.value = '';
            campo.classList.add('is-scrivendo');
            let i = 0;
            const passo = Math.max(1, Math.round(testo.length / 90));
            const t = setInterval(() => {
                i += passo;
                campo.value = testo.slice(0, i);
                campo.dispatchEvent(new Event('input', { bubbles: true }));
                if (i >= testo.length) { clearInterval(t); campo.classList.remove('is-scrivendo'); fine(); }
            }, 16);
        });

        const ai = $('[data-ai]', pagina);
        ai.addEventListener('click', async () => {
            const nome = form.nome.value.trim();
            if (!nome) { Lume.toast('Scrivi prima il nome del piatto', 'errore'); form.nome.focus(); return; }
            const ingredienti = $$('[data-riga]', ricetta)
                .map(r => $('[data-ingr]', r).selectedOptions[0])
                .filter(o => o && o.value).map(o => o.textContent.trim()).join(', ');
            const etichetta = $('span', ai);
            ai.classList.add('is-loading');
            etichetta.textContent = 'Sto scrivendo…';
            try {
                const json = await Lume.post(ai.dataset.url, {
                    nome, categoriaId: form.categoriaId.value || null, ingredienti, descrizione: form.descrizione.value
                });
                await scrivi(form.descrizione, json.descrizione || '');
                await scrivi(form.abbinamento, json.abbinamento || '');
                Lume.toast('Testi proposti dall\'AI: controllali prima di salvare');
            } catch (e) {
                Lume.toast(e.message, 'errore');
            } finally {
                ai.classList.remove('is-loading');
                etichetta.textContent = 'Scrivi con l\'AI';
            }
        });
    });

    /* ---------- Magazzino: ricerca, filtro e registrazione dei movimenti ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-magazzino]');
        if (!pagina) return;

        const righe = $$('.mag-tab tbody tr', pagina);
        const cerca = $('[data-mag-cerca]', pagina);
        const filtri = $$('[data-mag-filtro]', pagina);
        let soloSotto = new URLSearchParams(location.search).get('filtro') === 'sotto';

        const applica = () => {
            const q = cerca.value.trim().toLowerCase();
            let n = 0;
            righe.forEach(r => {
                const ok = (!soloSotto || r.dataset.sotto === 'true') && (!q || r.textContent.toLowerCase().includes(q));
                r.hidden = !ok;
                if (ok) n++;
            });
            filtri.forEach(f => f.classList.toggle('is-active', (f.dataset.magFiltro === 'sotto') === soloSotto));
            $('[data-mag-vuoto]', pagina).hidden = n > 0;
        };
        cerca.addEventListener('input', applica);
        filtri.forEach(f => f.addEventListener('click', () => { soloSotto = f.dataset.magFiltro === 'sotto'; applica(); }));
        applica();

        const modale = $('[data-modale-movimento]');
        const form = $('form', modale);
        const qta = $('[data-mov-qta]', modale);
        let scorta = 0, unita = '';
        const fmt = v => `${new Intl.NumberFormat('it-IT', { maximumFractionDigits: 3 }).format(v)} ${unita}`;
        const anteprima = () => {
            const tipo = form.querySelector('[name=tipo]:checked').value;
            const q = Number(qta.value) || 0;
            const dopo = tipo === 'CARICO' ? scorta + q : tipo === 'SPRECO' ? scorta - q : q;
            $('[data-mov-etichetta]', modale).textContent =
                { CARICO: 'Quantità arrivata', SPRECO: 'Quantità buttata', RETTIFICA: 'Quantità contata a mano' }[tipo];
            $('[data-mov-ora]', modale).textContent = fmt(scorta);
            const d = $('[data-mov-dopo]', modale);
            d.textContent = q ? fmt(dopo) : '—';
            d.style.color = dopo < 0 ? 'var(--danger)' : '';
        };
        modale.addEventListener('lume:apri', e => {
            const d = e.detail.dataset;
            form.reset();
            form.action = d.azione;
            scorta = Number(d.scorta);
            unita = d.unita;
            $('[data-mov-nome]', modale).textContent = d.nome;
            $('[data-mov-unita]', modale).textContent = unita;
            anteprima();
            setTimeout(() => qta.focus(), 50);
        });
        form.addEventListener('input', anteprima);
        form.addEventListener('change', anteprima);
    });

    /* ---------- Ordini ai fornitori: messaggio pronto da incollare ---------- */
    Lume.modulo(root => {
        if (root !== document) return;
        $$('[data-copia-ordine]').forEach(b => b.addEventListener('click', async () => {
            const righe = $$('[data-riga-ordine]', b.closest('.fornitore')).map(r => '- ' + r.dataset.rigaOrdine);
            const testo = `${b.dataset.testo}\n${righe.join('\n')}\nGrazie, Lume Ristorante`;
            try {
                await navigator.clipboard.writeText(testo);
                Lume.toast('Messaggio copiato: incollalo su WhatsApp o nella mail');
            } catch (e) { Lume.toast('Copia non disponibile in questo browser', 'errore'); }
        }));
        // unità di misura mostrata accanto ai campi della scheda ingrediente
        const scelta = $('[data-unita-scelta]');
        if (scelta) {
            const agg = () => {
                const u = scelta.querySelector('input:checked')?.nextElementSibling?.textContent || '';
                $$('[data-unita-label]').forEach(el => { el.textContent = u; });
            };
            scelta.addEventListener('change', agg);
            agg();
        }
    });

    /* ---------- Statistiche: analisi con l'AI ed esportazione CSV ---------- */
    Lume.modulo(root => {
        const pagina = root === document && $('[data-statistiche]');
        if (!pagina) return;

        const card = $('[data-ai-analisi]', pagina);
        const lista = $('[data-ai-punti]', card);
        const vuoto = $('[data-ai-vuoto]', card);
        const rifai = $('[data-ai-rifai]', card);
        const avvia = async () => {
            vuoto.hidden = true;
            rifai.hidden = true;
            lista.hidden = false;
            lista.innerHTML = '<li class="skeleton"></li>'.repeat(3);
            try {
                const json = await Lume.post(card.dataset.url, { giorni: pagina.dataset.giorni });
                lista.innerHTML = json.punti.slice(0, 5).map(p => `<li>${Lume.escape(p)}</li>`).join('');
                rifai.hidden = false;
            } catch (e) {
                lista.hidden = true;
                vuoto.hidden = false;
                Lume.toast(e.message, 'errore');
            }
        };
        $('[data-ai-avvia]', card).addEventListener('click', avvia);
        rifai.addEventListener('click', avvia);

        $('[data-esporta-csv]', pagina).addEventListener('click', () => {
            const punti = $('[data-csv="incassi"]', pagina)._punti || [];
            const righe = [['Giorno', 'Incasso (€)'], ...punti.map(p => [p.etichetta, String(p.valore).replace('.', ',')])];
            const csv = '﻿' + righe.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(';')).join('\r\n');
            const a = document.createElement('a');
            a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
            a.download = `lume-incassi-${pagina.dataset.giorni}-giorni.csv`;
            a.click();
            URL.revokeObjectURL(a.href);
        });
    });

    /* @@ALTRI_MODULI@@ */

    document.addEventListener('DOMContentLoaded', () => {
        const use = $('.side__logo use, svg use');
        Lume.icone = (use?.getAttribute('href') || '/img/icons.svg#x').split('#')[0];
        tema();
        laterale();
        popover();
        orologio();
        palette();
        conferme();
        ajax();
        modali();
        autoInvio();
        autoRefresh();
        attiva(document);
        flash();
    });
})();
