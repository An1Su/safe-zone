import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Order, OrderStatus } from '../../models/order.model';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';

interface SellerStats {
  totalOrders: number;
  pending: number;
  shipped: number;
  delivered: number;
  totalRevenue: number;
}

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './seller-orders.component.html',
  styleUrl: './seller-orders.component.scss',
})
export class SellerOrdersComponent implements OnInit {
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  loading = true;
  error = '';
  successMessage = '';
  
  // Product images cache
  productImages: Map<string, string> = new Map();
  
  // Stats
  stats: SellerStats = {
    totalOrders: 0,
    pending: 0,
    shipped: 0,
    delivered: 0,
    totalRevenue: 0,
  };

  // Filters
  searchQuery = '';
  statusFilter: OrderStatus | '' = '';
  
  // Status options for dropdown
  statusOptions: { value: OrderStatus | ''; label: string }[] = [
    { value: '', label: 'All Statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'READY_FOR_DELIVERY', label: 'Ready for Delivery' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  // Status update options (what seller can change to)
  nextStatusOptions: Record<OrderStatus, OrderStatus[]> = {
    'PENDING': ['READY_FOR_DELIVERY', 'CANCELLED'],
    'READY_FOR_DELIVERY': ['SHIPPED', 'CANCELLED'],
    'SHIPPED': ['DELIVERED'],
    'DELIVERED': [],
    'CANCELLED': [],
  };

  constructor(
    private readonly orderService: OrderService,
    private readonly mediaService: MediaService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';

    this.orderService.getSellerOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.applyFilters();
        this.calculateStats();
        this.loadProductImages();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load seller orders:', err);
        this.error = 'Failed to load orders. Please try again.';
        this.loading = false;
      },
    });
  }

  calculateStats(): void {
    this.stats = {
      totalOrders: this.orders.length,
      pending: this.orders.filter(o => o.status === 'PENDING' || o.status === 'READY_FOR_DELIVERY').length,
      shipped: this.orders.filter(o => o.status === 'SHIPPED').length,
      delivered: this.orders.filter(o => o.status === 'DELIVERED').length,
      totalRevenue: this.orders
        .filter(o => o.status !== 'CANCELLED')
        .reduce((sum, o) => sum + o.totalAmount, 0),
    };
  }

  loadProductImages(): void {
    const productIds = new Set<string>();
    this.orders.forEach(order => {
      order.items.forEach(item => productIds.add(item.productId));
    });

    productIds.forEach(productId => {
      this.mediaService.getMediaByProduct(productId).subscribe({
        next: (media) => {
          if (media.length > 0) {
            this.productImages.set(productId, this.mediaService.getMediaFile(media[0].id!));
          }
        },
        error: () => {
          // Silently fail for missing images
        },
      });
    });
  }

  getProductImage(productId: string): string | undefined {
    return this.productImages.get(productId);
  }

  applyFilters(): void {
    let result = [...this.orders];

    // Apply status filter
    if (this.statusFilter) {
      result = result.filter(o => o.status === this.statusFilter);
    }

    // Apply search filter
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      result = result.filter(o =>
        o.id?.toLowerCase().includes(query) ||
        o.items.some(item => item.productName.toLowerCase().includes(query)) ||
        o.shippingAddress.fullName.toLowerCase().includes(query)
      );
    }

    this.filteredOrders = result;
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.applyFilters();
  }

  updateOrderStatus(order: Order, newStatus: OrderStatus): void {
    if (!order.id) return;

    this.orderService.updateOrderStatus(order.id, newStatus).subscribe({
      next: (updatedOrder) => {
        // Update the order in the list
        const index = this.orders.findIndex(o => o.id === order.id);
        if (index !== -1) {
          this.orders[index] = updatedOrder;
          this.applyFilters();
          this.calculateStats();
        }
        this.successMessage = `Order status updated to ${this.getStatusLabel(newStatus)}`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Failed to update order status:', err);
        this.error = 'Failed to update order status. Please try again.';
        setTimeout(() => this.error = '', 3000);
      },
    });
  }

  canUpdateStatus(order: Order): boolean {
    return this.nextStatusOptions[order.status]?.length > 0;
  }

  getNextStatuses(order: Order): OrderStatus[] {
    return this.nextStatusOptions[order.status] || [];
  }

  getStatusClass(status: OrderStatus): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'READY_FOR_DELIVERY':
        return 'status-ready';
      case 'SHIPPED':
        return 'status-shipped';
      case 'DELIVERED':
        return 'status-delivered';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  getStatusLabel(status: OrderStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'READY_FOR_DELIVERY':
        return 'Ready for Delivery';
      case 'SHIPPED':
        return 'Shipped';
      case 'DELIVERED':
        return 'Delivered';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'N/A';
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  getOrderNumber(order: Order): string {
    if (!order.createdAt) return order.id?.slice(-8) || 'N/A';
    const date = typeof order.createdAt === 'string' ? new Date(order.createdAt) : order.createdAt;
    const year = date.getFullYear();
    const shortId = order.id?.slice(-3) || '000';
    return `ORD-${year}-${shortId}`;
  }
}

