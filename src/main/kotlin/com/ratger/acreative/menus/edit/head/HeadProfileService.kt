package com.ratger.acreative.menus.edit.head

class HeadProfileService(
    private val lookupService: LicensedProfileLookupService
) {

    fun lookupLicensedProfileAsync(name: String) = lookupService.lookupLicensedProfileAsync(name)

}
