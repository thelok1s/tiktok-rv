/*
 * Runtime verification for tiktok-rv patches.
 * Hooks the injected extension methods and logs when they fire + what they return.
 * Usage: frida -U -f com.zhiliaoapp.musically -l scripts/frida/verify.js
 *        (or attach: frida -U com.zhiliaoapp.musically -l scripts/frida/verify.js)
 */
'use strict';

function tag(s) { return '[RV-VERIFY] ' + s; }

Java.perform(function () {
    function hook(className, method, opts) {
        try {
            var C = Java.use(className);
            var overloads = C[method].overloads;
            overloads.forEach(function (ovl) {
                ovl.implementation = function () {
                    var ret = ovl.apply(this, arguments);
                    try {
                        var msg = className.split('.').pop() + '.' + method + '()';
                        if (opts && opts.logArgs && arguments.length) msg += ' arg0=' + arguments[0];
                        if (opts && opts.logRet) msg += ' -> ' + ret;
                        console.log(tag(msg));
                    } catch (e) {}
                    return ret;
                };
            });
            console.log(tag('hooked ' + className + '.' + method));
        } catch (e) {
            console.log(tag('MISS ' + className + '.' + method + ' : ' + e));
        }
    }

    // Downloads: path change + watermark removal
    hook('app.revanced.extension.tiktok.download.DownloadsPatch', 'getDownloadPath', { logRet: true });
    hook('app.revanced.extension.tiktok.download.DownloadsPatch', 'shouldRemoveWatermark', { logRet: true });

    // Feed filter: ads + others
    hook('app.revanced.extension.tiktok.feedfilter.FeedItemsFilter', 'filter', {});
    hook('app.revanced.extension.tiktok.feedfilter.AdsFilter', 'getFiltered', { logRet: true });

    // SIM spoof
    hook('app.revanced.extension.tiktok.spoof.sim.SpoofSimPatch', 'getCountryIso', { logRet: true });
    hook('app.revanced.extension.tiktok.spoof.sim.SpoofSimPatch', 'getOperator', { logRet: true });

    // Settings status (tells us which features the build considers enabled)
    hook('app.revanced.extension.tiktok.settings.SettingsStatus', 'load', {});

    console.log(tag('install complete'));
});
