#!/usr/bin/env python3
"""Exercise the packaged profile with Chromium and optionally capture its two themes."""
import argparse
import json
from mimetypes import guess_type
from pathlib import Path
from zipfile import ZipFile

from playwright.sync_api import expect, sync_playwright

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets"
URL = "https://profile.hunter.invalid"
PAGE = URL + "/profile-monarca/index.html"
SEED = {
    "editing": False,
    "photo": "",
    "week": [5] * 7,
    "gear": {"body": True},
    "fields": {
        "gender": "unspecified", "difficulty": "basic", "objective": "general",
        "trainingMode": "challenges", "dailyMissions": "3", "restSeconds": "60",
        "blockMode22": "rounds", "minutes": "30", "theme": "light", "outlineColor": "ocean",
        "collection": "hunter-male", "scene": "templo-del-ocaso", "companionCharacter": "knight",
    },
}


def check(browser, theme, width, capture, apk):
    page = browser.new_page(viewport={"width": width, "height": 856}, device_scale_factor=1,
                            is_mobile=True, has_touch=True)
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))

    def serve(route):
        name = route.request.url.removeprefix(URL + "/")
        if apk:
            try:
                content = apk.read("assets/" + name)
            except KeyError:
                route.abort()
                return
            route.fulfill(body=content, content_type=guess_type(name)[0])
            return
        asset = ROOT / name
        route.fulfill(path=asset) if asset.is_file() else route.abort()

    page.route(URL + "/**", serve)
    seed = {**SEED, "fields": {**SEED["fields"], "theme": theme}}
    edits = []
    page.expose_function("onProfileAction", lambda action: edits.append(action))
    page.add_init_script(f"""
        const seed = {json.dumps(seed)};
        window.HunterProfile = {{
          initial: () => JSON.stringify(seed),
          action: name => window.onProfileAction(name),
          submit: payload => {{ window.submittedProfile = JSON.parse(payload); window.profileResult(''); }}
        }};
    """)
    page.goto(PAGE)
    page.locator("h1").wait_for()
    expect(page.locator("#app")).to_have_attribute("data-dark", str(theme == "dark").lower())

    def screenshot(name):
        if capture:
            page.evaluate("document.fonts.ready")
            page.wait_for_load_state("networkidle")
            page.screenshot(path=capture / f"{theme}-{name}.png", animations="disabled")

    screenshot("01-identidad")
    page.get_by_role("button", name="Elegir fotografía").click()
    assert edits == ["photo"]
    next_step = page.locator('[data-do="next"]')
    next_step.click()
    expect(page.get_by_role("alert")).to_contain_text("Escribe tu nombre")
    page.locator('[data-field="name"]').fill("Alex")
    page.locator('[data-field="age"]').fill("14")
    next_step.click()
    screenshot("02-entrenamiento")
    page.get_by_role("button", name="Intermedio").click()
    page.locator('[data-picker="goal"]').click()
    expect(page.get_by_role("button", name="Mejorar mi movilidad")).to_be_visible()
    page.get_by_role("button", name="Mejorar mi movilidad").click()
    page.locator('[data-field="custom"]').fill("Caminar\nMejorar equilibrio")
    page.get_by_role("button", name="Rutinas").click()
    expect(page.locator('[data-picker="missions"]')).to_have_count(0)
    expect(page.locator('[data-picker="rest"]')).to_be_visible()
    expect(page.locator('[data-picker="format"]')).to_be_visible()
    page.locator('[data-picker="time"]').click()
    page.get_by_role("button", name="45 minutos").click()
    screenshot("02-rutinas")
    page.get_by_role("button", name="Misiones diarias").click()
    expect(page.locator('[data-picker="time"]')).to_have_count(0)
    page.locator('[data-picker="missions"]').click()
    expect(page.locator('[data-choice][data-key="missions"]')).to_have_count(7)
    page.get_by_role("button", name="3 misiones", exact=True).click()
    day = page.locator('[data-day="0"]')
    for label in ["Tren superior", "Tren central", "Tren inferior", "Movilidad", "Cardio", "Descanso", "Tren superior"]:
        day.click()
        expect(day).to_have_attribute("aria-label", f"Lunes: {label}")
    assert page.locator(".days").evaluate("(e) => e.scrollWidth === e.clientWidth")
    screenshot("02-calendario")
    page.locator('[data-gear="basic"][value="4"]').check()
    page.locator('[data-field="weights"]').fill("2, 6")
    page.locator('[data-gear="basic"][value="5"]').check()
    page.locator('[data-gear="gym"][value="4"]').check()
    next_step.click()
    screenshot("03-personalizacion")
    page.locator('[data-companion="3"]').click()
    expect(page.locator('.preview .companion-mark')).to_have_attribute("src", "../ui33/companion-tusk.webp")
    assert page.locator('[data-companion="3"] img').evaluate("(e) => e.complete && e.naturalWidth > 0")
    screenshot("03-tusk")
    for picker in ["figure", "scene"]:
        page.locator(f'[data-picker="{picker}"]').click()
        choice = page.locator(f'[data-key="{picker}"][data-choice="1"]')
        assert choice.locator("img").evaluate("(e) => e.decode().then(() => e.naturalWidth > 0)")
        choice.click()
    page.locator('[data-picker="theme"]').click()
    page.get_by_role("button", name="Negro" if theme == "light" else "Blanco").click()
    expect(page.locator("#app")).to_have_attribute("data-dark", str(theme != "dark").lower())
    page.locator('[data-picker="theme"]').click()
    page.get_by_role("button", name="Blanco" if theme == "light" else "Negro").click()
    next_step.click()
    screenshot("04-activacion")
    expect(next_step).to_be_disabled()
    assert page.locator("details:not([open])").count() == 3
    page.locator("summary").nth(1).click()
    expect(page.locator("details[open]")).to_contain_text("Mejorar mi movilidad")
    page.locator('[data-confirm]').check()
    next_step.click()
    expect(page.get_by_role("heading", name="Sistema activado")).to_be_visible()
    submitted = page.evaluate("window.submittedProfile")
    assert submitted["confirmed"] and submitted["week"] == [0, 5, 5, 5, 5, 5, 5]
    assert submitted["fields"]["companionCharacter"] == "tusk"
    assert submitted["fields"]["minutes"] == "45"
    assert submitted["fields"]["dumbbellWeights"] == "2, 6"
    assert submitted["gear"]["gym_bike"]
    assert submitted["fields"]["finalGoal"] == "Caminar\nMejorar equilibrio"
    page.get_by_role("button", name="ENTRAR AL SISTEMA").click()
    page.wait_for_function("window.submittedProfile !== undefined")
    assert edits == ["photo", "enter"], edits
    assert not errors, errors
    page.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--chromium", default="/usr/bin/chromium")
    parser.add_argument("--captures", type=Path)
    parser.add_argument("--apk", type=Path, help="Exercise assets as packaged in the APK")
    args = parser.parse_args()
    if args.captures:
        args.captures.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(executable_path=args.chromium, args=["--no-sandbox"])
        apk = ZipFile(args.apk) if args.apk else None
        try:
            for theme in ("light", "dark"):
                check(browser, theme, 428, args.captures, apk)
            check(browser, "light", 320, None, apk)
        finally:
            if apk:
                apk.close()
            browser.close()
    print("Perfil Monarca verificado en ambos temas y a 320 px.")
