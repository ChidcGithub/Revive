package com.music.revive.presentation.theme

import androidx.compose.ui.graphics.Color

// MARK: - Apple Music Primary Accent Colors
// Signature Apple Music vibrant red-pink for play buttons and active states
val AppleMusicPrimary = Color(0xFFFA2D48)
val AppleMusicPrimaryLight = Color(0xFFFF6B7A)
val AppleMusicPrimaryDark = Color(0xFFD41F3A)
val AppleMusicOnPrimary = Color(0xFFFFFFFF)

// Deep blue for secondary actions and links (Apple system blue)
val AppleMusicBlue = Color(0xFF007AFF)
val AppleMusicBlueLight = Color(0xFF409CFF)
val AppleMusicBlueDark = Color(0xFF0056B3)
val AppleMusicOnBlue = Color(0xFFFFFFFF)

// MARK: - Dark Mode Background Layers (Multi-layer approach)
// Pure black base
val AppleMusicBlack = Color(0xFF000000)
// Primary dark surface
val AppleMusicDarkSurface = Color(0xFF1C1C1E)
// Secondary dark surface (cards, elevated elements)
val AppleMusicDarkSurfaceSecondary = Color(0xFF2C2C2E)
// Tertiary dark surface (inputs, chips)
val AppleMusicDarkSurfaceTertiary = Color(0xFF3A3A3C)

// Dark text colors
val AppleMusicDarkPrimaryText = Color(0xFFF5F5F7)
val AppleMusicDarkSecondaryText = Color(0xFF86868B)
val AppleMusicDarkTertiaryText = Color(0xFF636366)
val AppleMusicDarkSeparator = Color(0xFF38383A)

// MARK: - Light Mode Background Layers
// Pure white base
val AppleMusicWhite = Color(0xFFFFFFFF)
// Primary light surface (off-white for depth)
val AppleMusicLightSurface = Color(0xFFF2F2F7)
// Secondary light surface (cards)
val AppleMusicLightSurfaceSecondary = Color(0xFFFFFFFF)
// Grouped background
val AppleMusicLightGroupedBackground = Color(0xFFF2F2F7)

// Light text colors
val AppleMusicLightPrimaryText = Color(0xFF1C1C1E)
val AppleMusicLightSecondaryText = Color(0xFF6E6E73)
val AppleMusicLightTertiaryText = Color(0xFF8E8E93)
val AppleMusicLightSeparator = Color(0xFFC6C6C8)

// MARK: - Apple Music Gradient Colors
// Vibrant gradient stops for ambient backgrounds
val GradientRedStart = Color(0xFFFF2D55)
val GradientRedEnd = Color(0xFFFF6B7A)

val GradientBlueStart = Color(0xFF007AFF)
val GradientBlueEnd = Color(0xFF5AC8FA)

val GradientPurpleStart = Color(0xFFAF52DE)
val GradientPurpleEnd = Color(0xFFD48AFF)

val GradientOrangeStart = Color(0xFFFF9500)
val GradientOrangeEnd = Color(0xFFFFCC00)

val GradientGreenStart = Color(0xFF34C759)
val GradientGreenEnd = Color(0xFF30D158)

// MARK: - Quality Badge Colors
// Lossless/Hi-Res badges
val BadgeLossless = Color(0xFFBF5AF2) // Purple
val BadgeHiRes = Color(0xFFFFD60A) // Gold
val BadgeDolbyAtmos = Color(0xFF0A84FF) // Blue
val BadgeExplicit = Color(0xFF8E8E93) // Gray

// MARK: - Lyrics Colors
// Karaoke effect colors
val LyricsActiveColor = Color(0xFFFFFFFF)
val LyricsInactiveColor = Color(0x80FFFFFF)
val LyricsKaraokeGradientStart = Color(0xFFFF2D55)
val LyricsKaraokeGradientEnd = Color(0xFFFF6B7A)

// Light mode lyrics
val LyricsActiveColorLight = Color(0xFF1C1C1E)
val LyricsInactiveColorLight = Color(0x801C1C1E)

// MARK: - Player Control Colors
// Progress bar
val ProgressBarActiveColor = AppleMusicPrimary
val ProgressBarInactiveColor = Color(0x40FFFFFF)
val ProgressBarActiveColorLight = AppleMusicPrimary
val ProgressBarInactiveColorLight = Color(0x40000000)

// Playback controls
val ControlButtonColor = Color(0xFFFFFFFF)
val ControlButtonColorDark = Color(0xFFFFFFFF)
val ControlButtonDisabled = Color(0x40FFFFFF)

// MARK: - Status Colors
val SuccessGreen = Color(0xFF34C759)
val WarningOrange = Color(0xFFFF9500)
val ErrorRed = Color(0xFFFF3B30)
val InfoBlue = Color(0xFF007AFF)

// MARK: - Smart Playlist Colors
val SmartPlaylistChillMix = Color(0xFF30D158)
val SmartPlaylistOnTheGoMix = Color(0xFFFF2D55)
val SmartPlaylistFavoritesMix = Color(0xFF007AFF)
val SmartPlaylistNewMusicMix = Color(0xFFAF52DE)

// MARK: - Legacy Compatibility Aliases
// Kept for backward compatibility with existing code
val md_theme_light_primary = AppleMusicPrimary
val md_theme_dark_primary = AppleMusicPrimaryLight
val md_theme_light_secondary = AppleMusicBlue
val md_theme_dark_secondary = AppleMusicBlueLight
val md_theme_light_background = AppleMusicLightSurface
val md_theme_dark_background = AppleMusicBlack
val md_theme_light_surface = AppleMusicLightSurfaceSecondary
val md_theme_dark_surface = AppleMusicDarkSurface
val md_theme_light_onPrimary = AppleMusicOnPrimary
val md_theme_dark_onPrimary = AppleMusicOnPrimary
val md_theme_light_onBackground = AppleMusicLightPrimaryText
val md_theme_dark_onBackground = AppleMusicDarkPrimaryText
val md_theme_light_onSurface = AppleMusicLightPrimaryText
val md_theme_dark_onSurface = AppleMusicDarkPrimaryText

// MARK: - Deprecated Legacy Colors
// These are kept only for full backward compatibility but should not be used in new code
val Purple80 = AppleMusicPrimaryLight
val PurpleGrey80 = AppleMusicBlueLight
val Pink80 = AppleMusicPrimaryLight
val Purple40 = AppleMusicPrimary
val PurpleGrey40 = AppleMusicBlue
val Pink40 = AppleMusicPrimary
val DarkPrimary = AppleMusicPrimaryLight
val DarkSecondary = AppleMusicBlueLight
val DarkBackground = AppleMusicBlack
val DarkSurface = AppleMusicDarkSurface
val DarkError = ErrorRed
val LightPrimary = AppleMusicPrimary
val LightSecondary = AppleMusicBlue
val LightBackground = AppleMusicLightSurface
val LightSurface = AppleMusicLightSurfaceSecondary
val LightError = ErrorRed

// Old player gradient colors (replaced by dynamic gradients)
val PlayerGradientStartLight = AppleMusicLightSurface
val PlayerGradientEndLight = AppleMusicLightSurfaceSecondary
val PlayerGradientStartDark = AppleMusicBlack
val PlayerGradientEndDark = AppleMusicDarkSurface

val NowPlayingWaveformActive = AppleMusicPrimary
val NowPlayingWaveformInactive = Color(0x4DFA2D48)
