//package com.xenonware.phone.ui.layouts
//
//import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.unit.IntSize
//import com.xenonware.phone.sign_in.SignInViewModel
//import com.xenonware.notes.ui.layouts.notes.CompactNotes
//import com.xenonware.notes.ui.layouts.notes.CoverNotes
//import com.xenonware.notes.viewmodel.LayoutType
//import com.xenonware.notes.viewmodel.NotesViewModel
//import com.xenonware.phone.viewmodel.LayoutType
//
//@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
//@Composable
//fun MainLayout(
//    viewModel: PhoneViewModel,
//    signInViewModel: SignInViewModel,
//    isLandscape: Boolean,
//    layoutType: LayoutType,
//    onOpenSettings: () -> Unit,
//    appSize: IntSize,
//) {
//
//    when (layoutType) {
//        LayoutType.COVER -> {
//            if (isLandscape) {
//                CoverPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = true,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            } else {
//                CoverPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = false,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            }
//        }
//
//        LayoutType.SMALL -> {
//            if (isLandscape) {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = true,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            } else {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = false,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            }
//        }
//
//        LayoutType.COMPACT -> {
//            if (isLandscape) {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = true,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            } else {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = false,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            }
//        }
//
//        LayoutType.MEDIUM -> {
//            if (isLandscape) {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = true,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            } else {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = false,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            }
//        }
//
//        LayoutType.EXPANDED -> {
//            if (isLandscape) {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = true,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            } else {
//                CompactPhone(
//                    viewModel = viewModel,
//                    signInViewModel = signInViewModel,
//                    isLandscape = false,
//                    layoutType = layoutType,
//                    onOpenSettings = onOpenSettings,
//                    appSize = appSize
//                )
//            }
//        }
//    }
//}
