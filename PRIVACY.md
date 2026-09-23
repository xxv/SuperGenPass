# Privacy Policy for Android SuperGenPass

*Last updated: 23 September 2026*

Android SuperGenPass is a hobby project. I wrote it because I wanted it, I give it
away for free, and I don't make any money from it. There's no company here, no
analytics dashboard, and nobody to sell your data to even if I wanted to.

So this policy is short, because there isn't much to say.

## The short version

**The app collects nothing and sends nothing anywhere.** It doesn't ask for the
`INTERNET` permission, which means Android itself will not let it talk to the
network, whatever I might have written in the code. You don't need to take my
word for it — the app is open source (GPLv3), so you can
[read the code](https://github.com/xxv/SuperGenPass) and check.

There are no accounts, no sign-ups, no ads, no analytics, no crash reporting, and
no third-party SDKs that report back to anyone.

## Your master password

Your master password is never stored. The app keeps it in memory only while you're
using it, and it's cleared when the screen turns off or after the timeout you set
in Settings.

Generated passwords aren't stored either. SuperGenPass doesn't keep a vault —
it recomputes each password from your master password and the domain, every time.

## What does get stored on your device

All of this stays in the app's private storage on your phone. None of it leaves
the device on its own.

  * **Your salt**, if you set one, in the app's preferences.
  * **Remembered domains** — the site names you've generated passwords for — in a
    local database, so the domain field can autocomplete. This is optional: turn
    off "Remember domains" in Settings, and you can wipe the list at any time with
    "Clear remembered domains".
  * **Your settings**: password length, hash type, PIN length, and so on.

Uninstalling the app removes all of it.

## Three things worth knowing

These are all things you choose to do, but they move data out of the app, so
they're worth being explicit about.

**Android backup.** The app allows Android's standard backup, which means your
settings, your salt, and your remembered domains may be included in your device's
backup to Google and restored onto a new phone. Your master password is not
included, because it is never stored in the first place. If you'd rather none of
this were backed up, you can turn off backup for the app in your device's system
settings.

**The clipboard.** If you copy a generated password, it goes onto the system
clipboard, which other apps on your device can read, and which on newer Android
versions may sync to your other devices. That's Android's clipboard behaving
normally, not something the app controls.

**NFC tags and QR codes.** If you use the NFC feature, you are writing a password
onto a physical tag that anyone holding the tag can read. If you scan a salt from a
QR code, the app hands off to whatever barcode scanner app you have installed;
that app is someone else's, with its own privacy policy.

## Permissions

The app requests exactly one permission: **NFC**, and only so it can read and write
password tags when you ask it to. That's it.

## Children

The app isn't directed at children and doesn't knowingly collect anything from
anyone, of any age, because it doesn't collect anything from anyone at all.

## If this policy changes

If I change it, I'll update this page and the date at the top. Since the app has no
network access, a change here would have to come with a new version of the app for
anything about your data to actually change.

## Questions

It's just me. Email <steve@staticfree.info>, or open an issue on
[GitHub](https://github.com/xxv/SuperGenPass/issues).
