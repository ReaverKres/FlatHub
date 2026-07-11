package io.flatzen.monetization.ads

/**
 * iOS AppLovin requires native framework (CocoaPods / SPM). Until linked + SDK key set,
 * this is a safe no-op so the KMP framework builds without AppLovin.
 * Wire real MAX SDK via cinterop or Swift bridge when keys are ready (see docs/12-console-setup.md).
 */
class IosAppLovinAdService : AdService by NoOpAdService()
