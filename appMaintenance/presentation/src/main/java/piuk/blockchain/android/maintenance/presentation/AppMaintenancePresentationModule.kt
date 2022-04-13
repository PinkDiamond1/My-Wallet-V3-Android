package piuk.blockchain.android.maintenance.presentation

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.maintenance.presentation.appupdateapi.AppUpdateSettingsImpl
import piuk.blockchain.android.maintenance.presentation.appupdateapi.InAppUpdateSettings

val appMaintenancePresentationModule = module {
    viewModel {
        AppMaintenanceSharedViewModel()
    }

    viewModel {
        AppMaintenanceViewModel(
            getAppMaintenanceConfigUseCase = get(),
            isDownloadInProgressUseCase = get()
        )
    }

    single<InAppUpdateSettings> {
        AppUpdateSettingsImpl(
            appUpdateManager = get(),
            appUpdateInfoFactory = get()
        )
    }
}

