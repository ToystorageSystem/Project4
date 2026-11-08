// Dependency wiring (DI) for the auth feature:
// datasource → repository → use cases.
// The ONLY presentation file allowed to import data/.
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/dio_client.dart';
import '../../../../core/network/session_store.dart';
import '../../data/datasources/auth_remote_data_source.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/change_password_usecase.dart';
import '../../domain/usecases/confirm_two_factor_usecase.dart';
import '../../domain/usecases/get_auth_status_usecase.dart';
import '../../domain/usecases/login_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import '../../domain/usecases/recover_with_code_usecase.dart';
import '../../domain/usecases/setup_two_factor_usecase.dart';
import '../../domain/usecases/verify_otp_usecase.dart';

// ---------- Data ----------

final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>(
  (ref) => AuthRemoteDataSource(ref.watch(dioProvider)),
);

/// Typed as the INTERFACE: the rest of the app never sees AuthRepositoryImpl
final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepositoryImpl(
    ref.watch(authRemoteDataSourceProvider),
    ref.watch(sessionStoreProvider),
  ),
);

// ---------- Use cases ----------

final loginUseCaseProvider =
    Provider((ref) => LoginUseCase(ref.watch(authRepositoryProvider)));

final verifyOtpUseCaseProvider =
    Provider((ref) => VerifyOtpUseCase(ref.watch(authRepositoryProvider)));

final recoverWithCodeUseCaseProvider =
    Provider((ref) => RecoverWithCodeUseCase(ref.watch(authRepositoryProvider)));

final getAuthStatusUseCaseProvider =
    Provider((ref) => GetAuthStatusUseCase(ref.watch(authRepositoryProvider)));

final changePasswordUseCaseProvider =
    Provider((ref) => ChangePasswordUseCase(ref.watch(authRepositoryProvider)));

final setupTwoFactorUseCaseProvider =
    Provider((ref) => SetupTwoFactorUseCase(ref.watch(authRepositoryProvider)));

final confirmTwoFactorUseCaseProvider =
    Provider((ref) => ConfirmTwoFactorUseCase(ref.watch(authRepositoryProvider)));

final logoutUseCaseProvider =
    Provider((ref) => LogoutUseCase(ref.watch(authRepositoryProvider)));
