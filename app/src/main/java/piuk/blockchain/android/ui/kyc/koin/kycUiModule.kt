@file:Suppress("USELESS_CAST")

package piuk.blockchain.android.ui.kyc.koin

import com.blockchain.koin.loqateFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressNextStepDecision
import piuk.blockchain.android.ui.kyc.address.KycHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.address.KycOldHomeAddressPresenter
import piuk.blockchain.android.ui.kyc.countryselection.KycCountrySelectionPresenter
import piuk.blockchain.android.ui.kyc.invalidcountry.KycInvalidCountryPresenter
import piuk.blockchain.android.ui.kyc.limits.KycLimitsInteractor
import piuk.blockchain.android.ui.kyc.limits.KycLimitsModel
import piuk.blockchain.android.ui.kyc.mobile.entry.KycMobileEntryPresenter
import piuk.blockchain.android.ui.kyc.mobile.validation.KycMobileValidationPresenter
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostPresenter
import piuk.blockchain.android.ui.kyc.profile.KycProfilePresenter
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecision
import piuk.blockchain.android.ui.kyc.reentry.ReentryDecisionKycNavigator
import piuk.blockchain.android.ui.kyc.reentry.TiersReentryDecision
import piuk.blockchain.android.ui.kyc.status.KycStatusPresenter
import piuk.blockchain.android.ui.kyc.tiersplash.KycTierSplashPresenter
import piuk.blockchain.android.ui.kyc.veriffsplash.VeriffSplashPresenter

val kycUiModule = module {

    scope(payloadScopeQualifier) {

        factory {
            TiersReentryDecision(
                dataRemediationService = get(),
                loqateFeatureFlag = get(loqateFeatureFlag),
            )
        }.bind(ReentryDecision::class)

        factory {
            ReentryDecisionKycNavigator(
                userService = get(),
                reentryDecision = get(),
                analytics = get()
            )
        }.bind(KycNavigator::class)

        factory {
            KycTierSplashPresenter(
                kycService = get(),
                analytics = get()
            )
        }

        factory {
            KycCountrySelectionPresenter(
                eligibilityService = get()
            )
        }

        factory {
            KycProfilePresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                userService = get(),
                stringUtils = get(),
            )
        }

        factory {
            KycHomeAddressPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                eligibilityService = get(),
                userService = get(),
                nabuUserSync = get(),
                custodialWalletManager = get(),
                kycNextStepDecision = get(),
                analytics = get(),
                kycTiersStore = get(),
            )
        }

        factory {
            KycOldHomeAddressPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                eligibilityService = get(),
                userService = get(),
                nabuUserSync = get(),
                custodialWalletManager = get(),
                kycNextStepDecision = get(),
                analytics = get(),
                kycTiersStore = get(),
            )
        }

        factory {
            KycMobileEntryPresenter(
                phoneNumberUpdater = get(),
                nabuUserSync = get()
            )
        }

        factory {
            KycMobileValidationPresenter(
                nabuUserSync = get(),
                phoneNumberUpdater = get(),
                dataRemediationService = get()
            )
        }

        factory {
            VeriffSplashPresenter(
                nabuToken = get(),
                nabuDataManager = get(),
                kycTiersStore = get(),
                analytics = get(),
                prefs = get()
            )
        }

        factory {
            KycStatusPresenter(
                nabuToken = get(),
                kycStatusHelper = get(),
                notificationTokenManager = get()
            )
        }

        factory {
            KycNavHostPresenter(
                nabuToken = get(),
                userService = get(),
                reentryDecision = get(),
                kycNavigator = get(),
                kycTiersStore = get(),
                getUserStore = get(),
                productEligibilityStore = get(),
                analytics = get()
            )
        }

        factory {
            KycInvalidCountryPresenter(
                nabuDataManager = get()
            )
        }

        factory {
            KycLimitsModel(
                interactor = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            KycLimitsInteractor(
                limitsDataManager = get(),
                kycService = get()
            )
        }
    }
}

val kycUiNabuModule = module {

    scope(payloadScopeQualifier) {

        factory {
            KycHomeAddressNextStepDecision(
                userService = get(),
                dataRemediationService = get()
            )
        }
    }
}
