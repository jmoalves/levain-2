# User Guide

Welcome to Levain 2! This guide will help you get started.

## Quick Start

### Installation

Learn how to download and install Levain 2:
- [Installation Guide](installation/INSTALLATION.md)

### Getting Started

Once installed, here are the most common tasks:

#### List Available Recipes

```bash
levain list
```

Filter by name:
```bash
levain list jdk
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
levain config add-repo my-recipes https://github.com/user/my-recipes

# List all configured repositories
levain config list-repo

# Remove a repository
levain config remove-repo my-recipes
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
3. Check your repository configuration with `levain config list-repo`

## Support

For bugs and feature requests, visit: https://github.com/jmoalves/levain-2

