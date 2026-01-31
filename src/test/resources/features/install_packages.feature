Feature: Install packages
  As a developer
  I want to install packages
  So that I can set up my development environment

  Scenario: Install a single package
    Given the install service is available
    When I install package "jdk-21"
    Then the package should be installed successfully

  Scenario: Install multiple packages
    Given the install service is available
    When I install packages "jdk-21" and "git"
    Then all packages should be installed successfully
