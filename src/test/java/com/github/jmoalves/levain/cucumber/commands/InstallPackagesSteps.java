package com.github.jmoalves.levain.cucumber.commands;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jmoalves.levain.service.InstallService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for installing packages.
 */
public class InstallPackagesSteps {

    private InstallService installService;
    private boolean installSuccessful;
    private Exception installException;

    @Given("the install service is available")
    public void theInstallServiceIsAvailable() {
        installService = new InstallService();
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
        assertTrue(installSuccessful, "Package installation should succeed");
        assertNull(installException, "No exception should be thrown");
    }

    @Then("all packages should be installed successfully")
    public void allPackagesShouldBeInstalledSuccessfully() {
        assertTrue(installSuccessful, "All packages should be installed successfully");
        assertNull(installException, "No exception should be thrown");
    }
}
