# tiktok-rv [RU]

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

Так как приложение состоит из нескольких частей (Split APKs), обычная установка через файловый менеджер не сработает. Выберите один из следующих способов:

### Вариант 1: С помощью SAI (Split APKs Installer) - Рекомендуется для Android
1. Скачайте и распакуйте ZIP-архив из [Releases](../../releases).
2. Установите приложение [SAI](https://play.google.com/store/apps/details?id=com.aefyr.sai) из Google Play или любого другого источника.
3. Откройте SAI, нажмите "Установить APK" -> "Встроенный файл-пикер" (или системный).
4. Выделите **все** извлечённые файлы (base.apk, config.arm64_v8a.apk, config.en.apk, config.xxhdpi.apk) и нажмите "Выбрать".
5. Нажмите "Установить".

### Вариант 2: С помощью ПК (ADB)
Если у вас есть компьютер и установлен ADB (Android Debug Bridge):
1. Распакуйте скачанный ZIP-архив в папку.
2. Подключите телефон к ПК с включённой отладкой по USB.
3. Выполните команду в терминале:
   ```bash
   adb install-multiple base.apk config.arm64_v8a.apk config.en.apk config.xxhdpi.apk
   ```

### Вариант 3: Root или Shizuku
Многие продвинутые менеджеры пакетов (например, App Manager, SAI) позволяют устанавливать наборы APK (сплиты) напрямую, если им предоставлен Root-доступ или доступ через Shizuku.

## Лицензия

Исходный код патчей в директории revanced-patches распространяется по лицензии GNU General Public License v3.0 (GPLv3), унаследованной от оригинального проекта  ReVanced Patches. Подробнее см. в файле LICENSE.

## Благодарности
* [gplaydl](https://github.com/rehmatworks/gplaydl)
* [revanced-cli](https://github.com/revanced/revanced-cli) 
* [revanced-patcher](https://github.com/ReVanced/revanced-patcher)
* [revanced-patches](https://gitlab.com/ReVanced/revanced-patches)


---

# tiktok-rv [EN]

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

Because the app consists of multiple parts (Split APKs), standard installation via a file manager will not work. Choose one of the following methods:

### Option 1: Using SAI (Split APKs Installer) - Recommended for Android
1. Download and extract the ZIP archive from [Releases](../../releases).
2. Install [SAI](https://play.google.com/store/apps/details?id=com.aefyr.sai) from Google Play or any other source.
3. Open SAI, tap "Install APKs" -> "Internal file picker" (or System file picker).
4. Select **all** extracted files (`base.apk`, `config.arm64_v8a.apk`, `config.en.apk`, `config.xxhdpi.apk`) and tap "Select".
5. Tap "Install".

### Option 2: Using PC (ADB)
If you have a computer with ADB (Android Debug Bridge) installed:
1. Extract the downloaded ZIP archive into a folder.
2. Connect your phone to your PC with USB Debugging enabled.
3. Run the following command in your terminal:
   ```bash
   adb install-multiple base.apk config.arm64_v8a.apk config.en.apk config.xxhdpi.apk
   ```

### Option 3: Root or Shizuku
Many advanced package managers (like App Manager) allow installing APK bundles (splits) directly if granted Root or Shizuku access.

## License

The patch source code in the `revanced-patches` directory is licensed under the GNU General Public License v3.0 (GPLv3), inherited from the original ReVanced Patches project. See the `LICENSE` file for more details.

## Credits
* [gplaydl](https://github.com/rehmatworks/gplaydl)
* [revanced-cli](https://github.com/revanced/revanced-cli) 
* [revanced-patcher](https://github.com/ReVanced/revanced-patcher)
* [revanced-patches](https://gitlab.com/ReVanced/revanced-patches)
