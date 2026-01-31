Feature: List available recipes
  As a developer
  I want to list available recipes
  So that I can see what packages are available for installation

  Scenario: List all recipes
    Given the recipe service is available
    When I request all recipes
    Then I should see a list of recipes
    And the list should contain "jdk-21"
    And the list should contain "git"

  Scenario: List recipes with filter
    Given the recipe service is available
    When I request recipes filtered by "jdk"
    Then I should see a filtered list of recipes
    And the list should contain "jdk-21"
    And the list should not contain "git"

  Scenario: List recipes with no matches
    Given the recipe service is available
    When I request recipes filtered by "nonexistent"
    Then I should see an empty list of recipes
