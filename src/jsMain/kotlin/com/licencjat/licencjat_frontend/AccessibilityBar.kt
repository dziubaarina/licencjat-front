package com.licencjat.licencjat_frontend

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

object AccessibilityBar {

    fun init() {
        FontSizeManager.init()
        ThemeManager.init()
        KeyboardManager.init()

        injectSkipLink()
        injectBar()
        adjustNavbarTop()
        observeBarHeight()
    }

    private fun injectSkipLink() {
        val a = document.createElement("a") as HTMLElement
        a.setAttribute("href", "#main-content")
        a.setAttribute("class", "skip-link")
        a.textContent = I18n.t("a11y.skipLink")
        document.body?.insertBefore(a, document.body?.firstChild)
    }

    private fun injectBar() {
        val bar = document.createElement("div") as HTMLElement
        bar.id = "a11y-bar"
        bar.setAttribute("role", "toolbar")
        bar.setAttribute("aria-label", "Accessibility options")
        bar.innerHTML = buildBarHtml()
        document.body?.insertBefore(bar, document.body?.firstChild?.nextSibling)
        attachEvents(bar)
    }

    private fun buildBarHtml(): String {
        val theme = ThemeManager.current
        val fsPct = FontSizeManager.currentPercent
        val lang  = I18n.lang

        fun active(th: AppTheme)  = if (theme == th) " active" else ""
        fun pressed(th: AppTheme) = (theme == th).toString()

        return """
        <span class="a11y-label">${I18n.t("a11y.fontSize")}</span>
        <div class="a11y-font-control">
            <button id="a11y-font-down" aria-label="Zmniejsz czcionkę" class="a11y-icon-btn">A<sup>-</sup></button>
            <input id="a11y-font-slider" type="range" min="80" max="200" step="10"
                   value="$fsPct" aria-label="Rozmiar czcionki" class="a11y-slider"/>
            <button id="a11y-font-up" aria-label="Zwiększ czcionkę" class="a11y-icon-btn">A<sup>+</sup></button>
            <span id="a11y-font-label" class="a11y-pct-label">${fsPct}%</span>
        </div>
        <div class="a11y-separator" role="separator"></div>
        <span class="a11y-label">${I18n.t("a11y.theme")}</span>
        <button id="a11y-theme-dark"   class="a11y-btn${active(AppTheme.DARK)}"          aria-pressed="${pressed(AppTheme.DARK)}">${I18n.t("a11y.dark")}</button>
        <button id="a11y-theme-light"  class="a11y-btn${active(AppTheme.LIGHT)}"         aria-pressed="${pressed(AppTheme.LIGHT)}">${I18n.t("a11y.light")}</button>
        <button id="a11y-theme-hc"     class="a11y-btn${active(AppTheme.HIGH_CONTRAST)}" aria-pressed="${pressed(AppTheme.HIGH_CONTRAST)}">${I18n.t("a11y.highContrast")}</button>
        <button id="a11y-theme-deuter" class="a11y-btn${active(AppTheme.DEUTERANOPIA)}"  aria-pressed="${pressed(AppTheme.DEUTERANOPIA)}">${I18n.t("a11y.deuteranopia")}</button>
        <div class="a11y-separator" role="separator"></div>
        <button id="a11y-lang" class="a11y-btn" aria-label="Zmień język / Change language">
            ${if (lang == "pl") "🇵🇱 PL" else "🇬🇧 EN"}
        </button>
        """.trimIndent()
    }

    private fun attachEvents(bar: HTMLElement) {
        val slider = bar.querySelector("#a11y-font-slider") as? HTMLElement
        val label  = bar.querySelector("#a11y-font-label")  as? HTMLElement

        bar.querySelector("#a11y-font-down")?.addEventListener("click", {
            FontSizeManager.decrease(); syncSlider(bar)
        })
        bar.querySelector("#a11y-font-up")?.addEventListener("click", {
            FontSizeManager.increase(); syncSlider(bar)
        })
        slider?.addEventListener("input", {
            val v = slider.asDynamic().value?.toString()?.toIntOrNull() ?: 100
            FontSizeManager.setExact(v)
            label?.textContent = "$v%"
        })

        mapOf(
            "#a11y-theme-dark"   to AppTheme.DARK,
            "#a11y-theme-light"  to AppTheme.LIGHT,
            "#a11y-theme-hc"     to AppTheme.HIGH_CONTRAST,
            "#a11y-theme-deuter" to AppTheme.DEUTERANOPIA,
        ).forEach { (sel, th) ->
            bar.querySelector(sel)?.addEventListener("click", {
                ThemeManager.setTheme(th); refreshThemeButtons(bar)
            })
        }

        // Płynna zmiana języka bez przeładowania strony!
        bar.querySelector("#a11y-lang")?.addEventListener("click", {
            val newLang = if (I18n.lang == "pl") "en" else "pl"
            I18n.lang = newLang

            // Natychmiastowa przebudowa paska, aby język i ikony zaktualizowały się bez odświeżania
            bar.innerHTML = buildBarHtml()
            attachEvents(bar)

            val msg = if (newLang == "en") "🇬🇧 Language successfully changed!" else "🇵🇱 Język zmieniony pomyślnie!"
            showInfoToast(msg)
        })
    }

    private fun syncSlider(bar: HTMLElement) {
        val pct = FontSizeManager.currentPercent
        (bar.querySelector("#a11y-font-slider") as? HTMLElement)?.asDynamic()?.value = pct
        (bar.querySelector("#a11y-font-label")  as? HTMLElement)?.textContent = "$pct%"
    }

    private fun refreshThemeButtons(bar: HTMLElement) {
        val cur = ThemeManager.current
        mapOf(
            "#a11y-theme-dark"   to AppTheme.DARK,
            "#a11y-theme-light"  to AppTheme.LIGHT,
            "#a11y-theme-hc"     to AppTheme.HIGH_CONTRAST,
            "#a11y-theme-deuter" to AppTheme.DEUTERANOPIA,
        ).forEach { (sel, th) ->
            val btn = bar.querySelector(sel) as? HTMLElement ?: return@forEach
            if (th == cur) { btn.classList.add("active"); btn.setAttribute("aria-pressed", "true") }
            else           { btn.classList.remove("active"); btn.setAttribute("aria-pressed", "false") }
        }
    }

    private fun showInfoToast(msg: String) {
        val toast = document.createElement("div") as HTMLElement
        toast.className = "dance-toast"
        toast.textContent = msg
        document.body?.appendChild(toast)
        window.setTimeout({
            toast.classList.add("dance-toast-hide")
            window.setTimeout({ document.body?.removeChild(toast) }, 400)
        }, 3500)
    }

    private fun doAdjust() {
        val bar    = document.getElementById("a11y-bar") as? HTMLElement ?: return
        val navbar = document.querySelector(".navbar.fixed-top") as? HTMLElement ?: return
        val h      = bar.getBoundingClientRect().height
        navbar.style.top = "${h}px"
        val navH   = navbar.getBoundingClientRect().height
        (document.body as? HTMLElement)?.style?.paddingTop = "${h + navH}px"
    }

    private fun adjustNavbarTop() {
        window.setTimeout({ doAdjust() }, 300)
    }

    private fun observeBarHeight() {
        val bar = document.getElementById("a11y-bar") as? HTMLElement ?: return
        val ro = js("""
            new ResizeObserver(function() {
                var bar    = document.getElementById('a11y-bar');
                var navbar = document.querySelector('.navbar.fixed-top');
                if (!bar || !navbar) return;
                var h = bar.getBoundingClientRect().height;
                navbar.style.top = h + 'px';
                var navH = navbar.getBoundingClientRect().height;
                document.body.style.paddingTop = (h + navH) + 'px';
            });
        """)
        ro.observe(bar)
    }
}