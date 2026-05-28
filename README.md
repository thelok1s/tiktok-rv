# tiktok-rv [RU]

[![VirusTotal Scan](https://img.shields.io/badge/VirusTotal-Scan_Result-blue?logo=virustotal)](https://github.com/thelok1s/tiktok-rv/releases/latest)

Этот репозиторий содержит автоматизированный GitHub Actions пайплайн для загрузки последней версии tiktok, патчинга, подписи и публикации мода каждые две недели.

## Что не так с другими модами?

Очень многое. Моддеры пиарят свои каналы и суют кучу рекламы, блокируя приложение баннерами и "обновлениями", делая загрузку новой версии длительным процессом с множеством лишних шагов, где по пути нужно подписаться на 1337 каналов и перейти по 999 скам сслыкам. Добавляют слишком много фич в достаточно хрупкое (из-за встроенных защит и обфускации) приложение, раздувая размер приложения.  

С другой стороны у нас есть мод от ReVanced (а точнее - сборная солянка из плагинов), где вы модифиуруете свое, официальное приложение, патчами с открытым кодом. Однако патчи давно не обновлялись, а последняя версия которая их поддерживала (36.1, типо того) безнадежно устарела. Этот репозиторий содержит базовый минимум из обновленных патчей и пайплайн для сборки мода.

<div style="display: flex; flex-wrap: wrap;">
  <img style="width: 33%;" width="710" height="357" alt="image" src="https://github.com/user-attachments/assets/effaaeac-7b47-48c3-9b45-2c9a5154ee5d" />
  <img style="width: 33%;" width="799" height="418" alt="image" src="https://github.com/user-attachments/assets/424c0a28-0e70-4417-9b7c-9811dcc8c9e5" />
  <img style="width: 30%;" width="801" height="459" alt="image" src="https://github.com/user-attachments/assets/80b21929-a4d8-45a8-aceb-3c6fc4af67de" />
   <p style="width: 100%;">
    2 популярных мода и плагин. PUP — это не вирус, а просто мусорный софт / bloatware (Potentially Unwanted Program)
  </p>
  <img style="width: 50%;" width="682" height="182" alt="image" src="https://github.com/user-attachments/assets/92a0610f-7f07-490a-a234-709da46d3505" />
  <p style="width: 100%;">
   Оригинальное приложение. И откуда только в модах взялась эта сигнатура? 
  </p>
</div>


## Обзор

### Применяемые патчи

Модифицированное TikTok-приложение принудительно включает следующие функции (меню настроек не предусмотрено):

* **Отключение требования входа:** Обходит обязательный экран входа/регистрации, позволяя сразу просматривать контент без аккаунта.
* **Фильтр ленты:** Удаляет рекламу из видеоленты.
* **Загрузки:** Принудительно включает скачивание всех видео (вероятно, функция сломана со стороны сервера), удаляет watermark TikTok из скачанных видео и изменяет директорию загрузки по умолчанию на /sdcard/Pictures/TikTok.
* **Скорость воспроизведения:** Добавляет управление скоростью воспроизведения (модифицировано для поддержки TikTok v45.3.3+ через обновление сигнатуры метода getCurrentAweme на LJII()).
* **Отображение seekbar:** Принудительно показывает seekbar видео, позволяя перематывать любые ролики.
* **Запоминание Clear Display:** Сохраняет выбранный режим «Clear Display» между видео.
* **Подмена SIM-региона:** Подменяет регион SIM-карты (по умолчанию — США) для обхода региональных ограничений контента. Загрузка контента работает.

### Pipeline выполняет следующие шаги:

1. Автоматически получает последние split APK-файлы TikTok напрямую из Google Play Store с помощью  [gplaydl](https://github.com/rehmatworks/gplaydl).
2. Использует Python-скрипт для парсинга AndroidManifest.xml базового APK и удаления android:isSplitRequired="true" вместе с metadata fused-модулей, обходя ошибку обязательных split APK без необходимости полной перекомпиляции ресурсов через apktool.
3. Компилирует модифицированное дерево исходников revanced-patches, включённое в этот репозиторий.
4. Использует [revanced-cli](https://github.com/revanced/revanced-cli) для внедрения патчей в байткод stripped Base APK.
5. Подписывает пропатченный Base APK и оригинальные configuration split APK (en, arm64_v8a, xxhdpi) с помощью PKCS12 keystore и упаковывает их в ZIP-архив, который затем загружается в GitHub Releases.


## Установка (Installation)

Так как приложение теперь собирается в единый Universal APK, установка стала максимально простой:

1. Скачайте файл `tiktok-rv.apk` из [Releases](../../releases).
2. Запустите скачанный файл и подтвердите установку. (Возможно, потребуется разрешить установку из неизвестных источников в настройках вашего устройства).

Вам больше не нужны SAI (Split APKs Installer) или ADB для установки нашего мода!

## Лицензия

Исходный код патчей в директории revanced-patches распространяется по лицензии GNU General Public License v3.0 (GPLv3), унаследованной от оригинального проекта  ReVanced Patches. Подробнее см. в файле LICENSE.

## Благодарности
* [gplaydl](https://github.com/rehmatworks/gplaydl)
* [revanced-cli](https://github.com/revanced/revanced-cli) 
* [revanced-patcher](https://github.com/ReVanced/revanced-patcher)
* [revanced-patches](https://gitlab.com/ReVanced/revanced-patches)


---

# tiktok-rv [EN]

[![VirusTotal Scan](https://img.shields.io/badge/VirusTotal-Scan_Result-blue?logo=virustotal)](https://github.com/thelok1s/tiktok-rv/releases/latest)

This repository contains an automated GitHub Actions pipeline for downloading the latest version of TikTok, patching it, signing it, and publishing the mod every two weeks.

## What's wrong with other mods?

A lot. Modders often promote their channels and inject a ton of ads, blocking the app with banners and "updates". They make downloading the new version a lengthy process with many unnecessary steps where you have to subscribe to multiple channels and click through scam links. They also add too many features to a fragile app (due to built-in protections and obfuscation), bloating the app size.

On the other hand, we have the ReVanced mod (or rather, a mix of plugins), where you modify your own official app with open-source patches. However, these patches haven't been updated in a long time, and the last version that supported them (around 36.1) is hopelessly outdated. This repository contains a basic minimum of updated patches and a pipeline to build the mod.

<div style="display: flex; flex-wrap: wrap;">
  <img style="width: 33%;" width="710" height="357" alt="image" src="https://github.com/user-attachments/assets/effaaeac-7b47-48c3-9b45-2c9a5154ee5d" />
  <img style="width: 33%;" width="799" height="418" alt="image" src="https://github.com/user-attachments/assets/424c0a28-0e70-4417-9b7c-9811dcc8c9e5" />
  <img style="width: 30%;" width="801" height="459" alt="image" src="https://github.com/user-attachments/assets/80b21929-a4d8-45a8-aceb-3c6fc4af67de" />
   <p style="width: 100%;">
    2 popular mods and plugin. PUP (Potentially Unwanted Program) — not necessarily a virus, just a bloatware
  </p>
  <img style="width: 50%;" width="682" height="182" alt="image" src="https://github.com/user-attachments/assets/92a0610f-7f07-490a-a234-709da46d3505" />
  <p style="width: 100%;">
   And original app. Where did these signatures came from?
  </p>
</div>

## Overview

### Applied Patches

The modified TikTok application permanently forces the following features (no settings menu is provided):

* **Disable login requirement:** Bypasses the mandatory login/sign-up screen, allowing you to view content immediately without an account.
* **Feed filter:** Removes advertisements from the video feed.
* **Downloads:** Force-enables downloading for all videos, removes the TikTok watermark from downloaded videos, and changes the default download directory to `/sdcard/Pictures/TikTok`.
* **Playback speed:** Adds playback speed controls (modified to support TikTok v45.3.3+ by updating the `getCurrentAweme` method signature to `LJII()`).
* **Show seekbar:** Forces the video seekbar to be visible, allowing you to scrub through any video.
* **Remember Clear Display:** Saves your chosen "Clear Display" mode across videos.
* **SIM spoof:** Spoofs the SIM card region (defaults to USA) to bypass regional content restrictions. Content loading works.

### The Pipeline executes the following steps:

1. Automatically fetches the latest split APKs for TikTok directly from the Google Play Store using [gplaydl](https://github.com/rehmatworks/gplaydl).
2. Uses a Python script to parse the `AndroidManifest.xml` of the base APK and removes `android:isSplitRequired="true"` along with fused module metadata. This bypasses the mandatory split APK error without needing full resource recompilation via `apktool`.
3. Compiles the modified `revanced-patches` source tree included in this repository.
4. Uses [revanced-cli](https://github.com/revanced/revanced-cli) to inject patches into the bytecode of the stripped Base APK.
5. Signs the patched Base APK and original configuration split APKs (en, arm64_v8a, xxhdpi) using a PKCS12 keystore and packages them into a ZIP archive uploaded to GitHub Releases.

## Installation

Because the app is now built as a single Universal APK, the installation is very simple:

1. Download the `tiktok-rv.apk` file from [Releases](../../releases).
2. Open the downloaded file and confirm the installation. (You may need to allow installation from unknown sources in your device settings).

You no longer need SAI (Split APKs Installer) or ADB to install this mod!

## License

The patch source code in the `revanced-patches` directory is licensed under the GNU General Public License v3.0 (GPLv3), inherited from the original ReVanced Patches project. See the `LICENSE` file for more details.

## Credits
* [apkeep](https://github.com/efforg/apkeep)
* [gplaydl](https://github.com/rehmatworks/gplaydl)
* [revanced-cli](https://github.com/revanced/revanced-cli) 
* [revanced-patcher](https://github.com/ReVanced/revanced-patcher)
* [revanced-patches](https://gitlab.com/ReVanced/revanced-patches)
