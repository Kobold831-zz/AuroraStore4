// IInstallResult.aidl
package com.aurora.store.data.service;

// Declare any non-default types here with import statements

interface IInstallResult {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void InstallSuccess(String packageName);
    void InstallFailure(String packageName, String errorString);
    void InstallError(String packageName, String errorString, String extra);
}