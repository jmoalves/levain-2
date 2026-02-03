package com.github.jmoalves.levain.cucumber.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jmoalves.levain.service.InstallService;
import com.github.jmoalves.levain.repository.Registry;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cucumber step definitions for installing packages.
 * Using ApplicationScoped to maintain state across steps within a scenario.
 */
@ApplicationScoped
public class InstallPackagesSteps {

    private final InstallService installService;
    private boolean installSuccessful;
    private Exception installException;

    @Inject
    public InstallPackagesSteps(InstallService installService) {
        this.installService = installService;
    }

    @Given("the install service is available")
    public void theInstallServiceIsAvailable() {
        // Clean registry before each test
        Registry registry = new Registry();
        registry.init();
        registry.clear();

        installSuccessful = false;
        installException = null;
    }

    @When("I install package {string}")
    public void iInstallPackage(String packageName) {
        try {
            installService.install(packageName);
            installSuccessful = true;
        } catch (Exception e) {
            installException = e;
            installSuccessful = false;
        }
    }

    @When("I install packages {string} and {string}")
    public void iInstallPackages(String package1, String package2) {
        try {
            installService.install(package1);
            installService.install(package2);
            installSuccessful = true;
        } catch (Exception e) {
            installException = e;
            installSuccessful = false;
        }
    }

    @Then("the package should be installed successfully")
    public void thePackageShouldBeInstalledSuccessfully() {
        if (installException != null) {
            throw new AssertionError("Package installation failed with exception: " + installException.getMessage(),
                    installException);
        }
        assertTrue(installSuccessful, "Package installation should succeed");
    }

    @Then("all packages should be installed successfully")
    public void allPackagesShouldBeInstalledSuccessfully() {
        if (installException != null) {
            throw new AssertionError("Package installation failed with exception: " + installException.getMessage(),
                    installException);
        }
        assertTrue(installSuccessful, "All packages should be installed successfully");
    }
}
