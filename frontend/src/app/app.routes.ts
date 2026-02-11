import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login.component';
import { RegisterComponent } from './components/auth/register.component';
import { CartComponent } from './components/cart/cart.component';
import { CheckoutComponent } from './components/checkout/checkout.component';
import { HomeComponent } from './components/home/home.component';
import { OrderConfirmationComponent } from './components/order-confirmation/order-confirmation.component';
import { Analytics } from './components/analytics/analytics';
import { OrderHistoryComponent } from './components/order-history/order-history.component';
import { ProductDetailComponent } from './components/products/product-detail.component';
import { ProductListComponent } from './components/products/product-list.component';
import { UserProfileComponent } from './components/profile/user-profile.component';
import { SellerDashboardComponent } from './components/seller/seller-dashboard.component';
import { SellerOrdersComponent } from './components/seller/seller-orders.component';
import { authGuard } from './guards/auth.guard';
import { sellerGuard } from './guards/role.guard';

export const routes: Routes = [
  // Public routes
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'products', component: ProductListComponent },
  { path: 'products/:id', component: ProductDetailComponent },

  // Cart - accessible to everyone (shows login prompt if not authenticated)
  { path: 'cart', component: CartComponent },

  // Checkout & Orders - require authentication
  {
    path: 'checkout',
    component: CheckoutComponent,
    canActivate: [authGuard],
  },
  {
    path: 'order-confirmation/:id',
    component: OrderConfirmationComponent,
    canActivate: [authGuard],
  },
  {
    path: 'my-orders',
    component: OrderHistoryComponent,
    canActivate: [authGuard],
  },
  {
    path: 'analytics',
    component: Analytics,
    canActivate: [authGuard],
  },

  // Protected routes - require authentication
  {
    path: 'profile',
    component: UserProfileComponent,
    canActivate: [authGuard],
  },

  // Seller-only routes - require authentication + seller role
  {
    path: 'seller/dashboard',
    component: SellerDashboardComponent,
    canActivate: [sellerGuard],
  },
  {
    path: 'seller/orders',
    component: SellerOrdersComponent,
    canActivate: [sellerGuard],
  },

  // Wildcard - redirect to home
  { path: '**', redirectTo: '' },
];
