package com.github.jmoalves.levain.cucumber.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.github.jmoalves.levain.service.RecipeService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for listing recipes.
 */
public class ListRecipesSteps {

    private RecipeService recipeService;
    private List<String> recipes;

    @Given("the recipe service is available")
    public void theRecipeServiceIsAvailable() {
        recipeService = new RecipeService();
    }

    @When("I request all recipes")
    public void iRequestAllRecipes() {
        recipes = recipeService.listRecipes(null);
    }

    @When("I request recipes filtered by {string}")
    public void iRequestRecipesFilteredBy(String filter) {
        recipes = recipeService.listRecipes(filter);
    }

    @Then("I should see a list of recipes")
    public void iShouldSeeAListOfRecipes() {
        assertNotNull(recipes);
        assertFalse(recipes.isEmpty());
    }

    @Then("I should see a filtered list of recipes")
    public void iShouldSeeAFilteredListOfRecipes() {
        assertNotNull(recipes);
        assertFalse(recipes.isEmpty());
    }

    @Then("I should see an empty list of recipes")
    public void iShouldSeeAnEmptyListOfRecipes() {
        assertNotNull(recipes);
        assertTrue(recipes.isEmpty());
    }

    @Then("the list should contain {string}")
    public void theListShouldContain(String recipeName) {
        assertTrue(recipes.contains(recipeName),
                "Expected recipe list to contain: " + recipeName);
    }

    @Then("the list should not contain {string}")
    public void theListShouldNotContain(String recipeName) {
        assertFalse(recipes.contains(recipeName),
                "Expected recipe list to not contain: " + recipeName);
    }
}
