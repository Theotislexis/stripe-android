package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetModule::class,
        CoroutineContextModule::class,
        LoggingModule::class,
        NavigationModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeComponent {
    val viewModel: FinancialConnectionsSheetNativeViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
