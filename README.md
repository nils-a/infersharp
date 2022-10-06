# IntelliJ-Infersharp

![Build](https://github.com/nils-a/infersharp/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20093-infersharp.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20093-infersharp.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<!-- Plugin description -->
This IntelliJ Platform Plugin provides static code analysis of your
project using [Microsoft Infer#](https://github.com/microsoft/infersharp).

Currently, it is based on WSL. **_This means it is windows-only for now_**. 

<!-- Plugin description end -->

## Installation of the Plugin

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "infersharp"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/nils-a/infersharp/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Features in planning

in no particular order:

- support for calling infer# via docker (should work on all systems)
- support for calling a locally installed infer# (should work on linux)
- Show errors "in code"
- Show errors by loading the sarif file manually
  (i.e. someone provided a bug report and attached the sarif output of infer#)
- support in Rider: Right-click on a project file -> the build output folder should be known.
  No need to ask for a folder.

## installation of Infer#

Should be semi-automated, using <kbd>Tools</kbd> > <kbd>Infer#</kbd> > <kbd>Install (WSL)</kbd>
TODO: Document the manual steps.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
