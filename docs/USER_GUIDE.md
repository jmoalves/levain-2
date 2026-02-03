# User Guide

Welcome to Levain 2! This guide will help you get started.

## Quick Start

### Installation

Learn how to download and install Levain 2:
- [Installation Guide](installation/INSTALLATION.md)

### Getting Started

Once installed, here are the most common tasks:

#### List Available Recipes

**Show all recipes with installation status:**
```bash
levain list
```

This displays all available recipes with `[installed]` indicators next to installed ones.

**Filter by name pattern:**
```bash
levain list jdk              # Show JDK recipes
levain list node             # Show Node.js recipes
```

**Show only installed recipes:**
```bash
levain list --installed      # List all installed recipes
levain list jdk --installed  # List only installed JDK recipes
```

**Show only available (not installed) recipes:**
```bash
levain list --available      # Show recipes that are not yet installed
levain list jdk --available  # Show uninstalled JDK recipes
```

#### Install Packages

```bash
levain install jdk-21
```

Install multiple packages:
```bash
levain install jdk-21 git maven
```

#### Open a Shell

Launch a new shell with installed packages in the environment:
```bash
levain shell jdk-21 maven
```

### Configuration

Manage your repositories and settings:

```bash
# Add a custom recipe repository
levain config repo add https://github.com/user/my-recipes my-recipes

# List all configured repositories
levain config repo list

# Remove a repository
levain config repo remove my-recipes
```

See [Configuration](configuration/CONFIG_IMPLEMENTATION.md) for more details.

## Advanced Topics

### Nexus Repository Integration

For enterprise environments with Nexus repositories:
- [Basic Setup](setup/NEXUS_SIMPLE_SETUP.md)
- [Full Setup Guide](setup/NEXUS_SETUP.md)
- [Integration Details](setup/NEXUS_INTEGRATION.md)

## Troubleshooting

If you encounter issues:

1. Check that your system meets the [installation prerequisites](installation/INSTALLATION.md)
2. Verify you have the correct Java version installed
3. Check your repository configuration with `levain config repo list`

## Support

For bugs and feature requests, visit: https://github.com/jmoalves/levain-2

