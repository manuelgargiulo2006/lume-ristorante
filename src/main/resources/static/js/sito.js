/* Lume · comportamenti del sito pubblico (nessuna libreria esterna) */
(() => {
    'use strict';

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

    /* ---------- barra di navigazione ---------- */
    function nav() {
        const bar = $('[data-nav]');
        if (!bar) return;
        const aggiorna = () => bar.classList.toggle('is-scrolled', window.scrollY > 24);
        aggiorna();
        window.addEventListener('scroll', aggiorna, { passive: true });

        const toggle = $('[data-nav-toggle]', bar);
        toggle?.addEventListener('click', () => {
            const aperto = bar.classList.toggle('is-open');
            toggle.setAttribute('aria-expanded', String(aperto));
        });
    }

    /* ---------- comparsa degli elementi allo scroll ---------- */
    function reveal() {
        const elementi = $$('.reveal');
        if (!('IntersectionObserver' in window)) {
            elementi.forEach(el => el.classList.add('is-visible'));
            return;
        }
        const io = new IntersectionObserver(voci => {
            voci.forEach(v => {
                if (v.isIntersecting) {
                    v.target.classList.add('is-visible');
                    io.unobserve(v.target);
                }
            });
        }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });
        elementi.forEach((el, i) => {
            el.style.transitionDelay = `${Math.min(i % 6, 5) * 70}ms`;
            io.observe(el);
        });
    }

    /* ---------- menù: filtri e categoria attiva ---------- */
    function menu() {
        const barra = $('[data-menu-bar]');
        if (!barra) return;

        const voci = $$('.voce');
        const sezioni = $$('.menu-sez');
        const vuoto = $('[data-menu-vuoto]');
        const chips = $$('[data-filtro]', barra);
        const mare = ['PESCE', 'CROSTACEI', 'MOLLUSCHI'];

        const applica = () => {
            const attivi = chips.filter(c => c.getAttribute('aria-pressed') === 'true').map(c => c.dataset.filtro);
            let visibili = 0;
            voci.forEach(v => {
                const allergeni = (v.dataset.allergeni || '').split(' ').filter(Boolean);
                const ok = attivi.every(f => {
                    if (f === 'veg') return v.dataset.veg === 'true';
                    if (f === 'mare') return !allergeni.some(a => mare.includes(a));
                    return !allergeni.includes(f);
                });
                v.classList.toggle('is-nascosta', !ok);
                if (ok) visibili++;
            });
            sezioni.forEach(s => { s.hidden = !$$('.voce:not(.is-nascosta)', s).length; });
            vuoto.hidden = visibili > 0;
        };

        chips.forEach(c => c.addEventListener('click', () => {
            c.setAttribute('aria-pressed', String(c.getAttribute('aria-pressed') !== 'true'));
            applica();
        }));
        $('[data-filtri-reset]')?.addEventListener('click', () => {
            chips.forEach(c => c.setAttribute('aria-pressed', 'false'));
            applica();
        });

        // evidenzia la categoria che si sta leggendo
        const link = $$('.menu-bar__cats a', barra);
        const io = new IntersectionObserver(entries => {
            entries.forEach(e => {
                if (!e.isIntersecting) return;
                link.forEach(a => {
                    const attivo = a.getAttribute('href') === `#${e.target.id}`;
                    a.classList.toggle('is-active', attivo);
                    if (attivo) {
                        const cont = a.parentElement;
                        cont.scrollTo({ left: a.offsetLeft - cont.clientWidth / 2 + a.clientWidth / 2, behavior: 'smooth' });
                    }
                });
            });
        }, { rootMargin: '-35% 0px -60% 0px' });
        sezioni.forEach(s => io.observe(s));
    }

    /* ---------- prenotazione: orari in tempo reale ---------- */
    function prenota() {
        const form = $('[data-prenota]');
        if (!form) return;

        const api = form.dataset.api;
        const corpo = $('[data-orari-body]', form);
        const rPersone = $('[data-r-persone]');
        const rData = $('[data-r-data]');
        const rOra = $('[data-r-ora]');
        const rGruppo = $('[data-r-gruppo]');
        const fmtGiorno = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric', month: 'long' });

        const valore = nome => form.querySelector(`input[name="${nome}"]:checked`)?.value;
        let richiesta = 0;

        const riepilogo = () => {
            const persone = Number(valore('persone') || 2);
            rPersone.textContent = `${persone} ${persone === 1 ? 'persona' : 'persone'}`;
            const data = valore('data');
            rData.textContent = data ? fmtGiorno.format(new Date(`${data}T12:00:00`)) : 'Scegli il giorno';
            rOra.textContent = valore('ora') || 'orario';
            rGruppo.hidden = persone < 7;
        };

        const slot = (f, scelta, persone) => `
            <label class="slot${f.disponibile ? '' : ' is-pieno'}">
                <input type="radio" name="ora" value="${f.ora}"${f.ora === scelta && f.disponibile ? ' checked' : ''}${f.disponibile ? '' : ' disabled'}>
                <span class="slot__ora">${f.ora}</span>
                <span class="slot__posti">${f.disponibile ? (f.postiLiberi <= 8 ? 'ultimi posti' : 'disponibile') : (f.postiLiberi >= persone ? 'non prenotabile' : 'completo')}</span>
            </label>`;

        const carica = async () => {
            const data = valore('data');
            const persone = valore('persone') || 2;
            if (!data) return;
            const id = ++richiesta;
            const scelta = valore('ora');
            corpo.classList.add('is-loading');
            try {
                const r = await fetch(`${api}?data=${data}&persone=${persone}`, { headers: { Accept: 'application/json' } });
                if (!r.ok) throw new Error(r.status);
                const json = await r.json();
                if (id !== richiesta) return;
                if (json.chiuso) {
                    corpo.innerHTML = '<p class="orari__vuoto">Il lunedì siamo chiusi per riposo. Scegli un altro giorno.</p>';
                } else if (!json.fasce.some(f => f.disponibile)) {
                    corpo.innerHTML = `<p class="orari__vuoto">Per questo giorno non ci sono più tavoli per ${persone} persone. Prova un'altra data.</p>`;
                } else {
                    const gruppi = ['Pranzo', 'Cena'].map(s => [s, json.fasce.filter(f => f.servizio === s)]).filter(([, l]) => l.length);
                    corpo.innerHTML = `<div class="servizio">${gruppi.map(([nome, lista]) => `
                        <div class="servizio__gruppo"><h4>${nome}</h4>
                        <div class="slots">${lista.map(f => slot(f, scelta, Number(persone))).join('')}</div></div>`).join('')}</div>`;
                }
            } catch (e) {
                corpo.innerHTML = '<p class="orari__vuoto">Non riesco a caricare gli orari. Riprova tra un momento.</p>';
            } finally {
                if (id === richiesta) corpo.classList.remove('is-loading');
                riepilogo();
            }
        };

        form.addEventListener('change', e => {
            if (e.target.name === 'data' || e.target.name === 'persone') carica();
            riepilogo();
        });

        // porta in vista il giorno scelto nella striscia
        const striscia = $('[data-giorni]', form);
        const scelto = $('.giorno input:checked', form)?.closest('.giorno');
        if (striscia && scelto) {
            striscia.scrollLeft = scelto.offsetLeft - striscia.offsetLeft - striscia.clientWidth / 2 + scelto.clientWidth / 2;
        }
    }

    /* ---------- piccole utilità ---------- */
    function copia() {
        $$('[data-copia-btn]').forEach(b => b.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(b.dataset.testo);
                const prima = b.textContent;
                b.textContent = 'Copiato ✓';
                setTimeout(() => { b.textContent = prima; }, 1800);
            } catch (e) { /* appunti non disponibili */ }
        }));
    }

    function login() {
        const pwd = $('[data-pwd]');
        $('[data-pwd-toggle]')?.addEventListener('click', e => {
            const visibile = pwd.type === 'text';
            pwd.type = visibile ? 'password' : 'text';
            e.currentTarget.textContent = visibile ? 'Mostra' : 'Nascondi';
        });
        $$('[data-demo]').forEach(b => b.addEventListener('click', () => {
            const form = b.closest('form');
            form.username.value = b.dataset.demo;
            form.password.value = b.dataset.pass;
            form.requestSubmit();
        }));
    }

    document.addEventListener('DOMContentLoaded', () => {
        nav();
        reveal();
        menu();
        prenota();
        copia();
        login();
    });
})();
