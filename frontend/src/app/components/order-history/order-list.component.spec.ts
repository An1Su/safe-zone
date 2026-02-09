import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { Order, OrderStatus } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';
import { OrderListComponent } from './order-list.component';

describe('OrderListComponent', () => {
  let component: OrderListComponent;
  let fixture: ComponentFixture<OrderListComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const createBuyerMockOrders = (): Order[] => [
    {
      id: 'order-001',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-1',
          productName: 'Velvet Matte Lipstick',
          sellerId: 'seller-1',
          price: 24.99,
          quantity: 2,
        },
        {
          productId: 'prod-2',
          productName: 'Rose Blush Palette',
          sellerId: 'seller-1',
          price: 39.99,
          quantity: 1,
        },
      ],
      status: 'DELIVERED',
      totalAmount: 89.97,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-25'),
    },
    {
      id: 'order-002',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-3',
          productName: 'Luxe Mascara',
          sellerId: 'seller-2',
          price: 29.99,
          quantity: 1,
        },
      ],
      status: 'SHIPPED',
      totalAmount: 29.99,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-20'),
    },
    {
      id: 'order-003',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-4',
          productName: 'Sunset Eyeshadow Palette',
          sellerId: 'seller-1',
          price: 54.99,
          quantity: 1,
        },
      ],
      status: 'PENDING',
      totalAmount: 54.99,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-15'),
    },
  ];

  const createSellerMockOrders = (): Order[] => [
    {
      id: 'order-1',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-1',
          productName: 'Lipstick',
          sellerId: 'seller-1',
          price: 25,
          quantity: 2,
        },
      ],
      status: 'PENDING',
      totalAmount: 50,
      shippingAddress: {
        fullName: 'Jane Doe',
        address: '123 Main St',
        city: 'New York',
        phone: '555-1234',
      },
      createdAt: new Date('2024-01-15'),
    },
    {
      id: 'order-2',
      userId: 'user-2',
      items: [
        {
          productId: 'prod-2',
          productName: 'Foundation',
          sellerId: 'seller-1',
          price: 45,
          quantity: 1,
        },
      ],
      status: 'SHIPPED',
      totalAmount: 45,
      shippingAddress: {
        fullName: 'John Smith',
        address: '456 Oak Ave',
        city: 'Los Angeles',
        phone: '555-5678',
      },
      createdAt: new Date('2024-01-10'),
    },
    {
      id: 'order-3',
      userId: 'user-3',
      items: [
        {
          productId: 'prod-3',
          productName: 'Mascara',
          sellerId: 'seller-1',
          price: 20,
          quantity: 3,
        },
      ],
      status: 'DELIVERED',
      totalAmount: 60,
      shippingAddress: {
        fullName: 'Alice Johnson',
        address: '789 Pine Rd',
        city: 'Chicago',
        phone: '555-9012',
      },
      createdAt: new Date('2024-01-05'),
    },
    {
      id: 'order-4',
      userId: 'user-4',
      items: [
        {
          productId: 'prod-4',
          productName: 'Eyeshadow',
          sellerId: 'seller-1',
          price: 30,
          quantity: 1,
        },
      ],
      status: 'CANCELLED',
      totalAmount: 30,
      shippingAddress: {
        fullName: 'Bob Wilson',
        address: '321 Elm St',
        city: 'Houston',
        phone: '555-3456',
      },
      createdAt: new Date('2024-01-01'),
    },
  ];

  beforeEach(async () => {
    const orderSpy = jasmine.createSpyObj('OrderService', [
      'getOrders',
      'getSellerOrders',
      'cancelOrder',
      'updateOrderStatus',
    ]);
    const mediaSpy = jasmine.createSpyObj('MediaService', [
      'getMediaByProduct',
      'getMediaFile',
    ]);
    const authSpy = jasmine.createSpyObj('AuthService', ['isSeller', 'isClient']);

    mediaSpy.getMediaByProduct.and.returnValue(of([]));
    mediaSpy.getMediaFile.and.returnValue('http://example.com/image.jpg');

    await TestBed.configureTestingModule({
      imports: [OrderListComponent, RouterTestingModule, FormsModule],
      providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: MediaService, useValue: mediaSpy },
        { provide: AuthService, useValue: authSpy },
      ],
    }).compileComponents();

    orderServiceSpy = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    mediaServiceSpy = TestBed.inject(MediaService) as jasmine.SpyObj<MediaService>;
    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    // Verify services are configured
    expect(mediaServiceSpy.getMediaByProduct).toBeDefined();
  });

  describe('Buyer Mode', () => {
    beforeEach(() => {
      authServiceSpy.isSeller.and.returnValue(false);
      orderServiceSpy.getOrders.and.returnValue(of(createBuyerMockOrders()));
      fixture = TestBed.createComponent(OrderListComponent);
      component = fixture.componentInstance;
    });

    it('should create', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
      expect(component.isSeller).toBeFalse();
    });

    it('should load buyer orders on init', () => {
      fixture.detectChanges();
      expect(orderServiceSpy.getOrders).toHaveBeenCalled();
      expect(orderServiceSpy.getSellerOrders).not.toHaveBeenCalled();
      expect(component.orders.length).toBe(3);
      expect(component.displayOrders.length).toBe(3);
    });

    it('should sort orders by date descending', () => {
      fixture.detectChanges();
      expect(component.orders[0].id).toBe('order-001');
      expect(component.orders[1].id).toBe('order-002');
      expect(component.orders[2].id).toBe('order-003');
    });

    it('should calculate buyer stats correctly', () => {
      fixture.detectChanges();
      expect(component.buyerStats.totalOrders).toBe(3);
      expect(component.buyerStats.delivered).toBe(1);
      expect(component.buyerStats.inProgress).toBe(2);
      expect(component.buyerStats.totalSpent).toBe(174.95);
    });

    it('should not count cancelled orders in total spent', () => {
      const ordersWithCancelled: Order[] = [
        ...createBuyerMockOrders(),
        {
          id: 'order-004',
          userId: 'user-1',
          items: [],
          status: 'CANCELLED',
          totalAmount: 100,
          shippingAddress: {
            fullName: 'Test',
            address: 'Test',
            city: 'Test',
            phone: '123',
          },
          createdAt: new Date(),
        },
      ];
      orderServiceSpy.getOrders.and.returnValue(of(ordersWithCancelled));
      fixture.detectChanges();
      expect(component.buyerStats.totalOrders).toBe(4);
      expect(component.buyerStats.totalSpent).toBe(174.95);
    });

    it('should allow cancelling pending orders', () => {
      fixture.detectChanges();
      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      const cancelledOrder = { ...pendingOrder, status: 'CANCELLED' as OrderStatus };
      orderServiceSpy.cancelOrder.and.returnValue(of(cancelledOrder));
      spyOn(window, 'confirm').and.returnValue(true);

      component.cancelOrder(pendingOrder);

      expect(orderServiceSpy.cancelOrder).toHaveBeenCalledWith(pendingOrder.id!);
      const updatedOrder = component.orders.find((o) => o.id === pendingOrder.id);
      expect(updatedOrder?.status).toBe('CANCELLED');
    });

    it('should update displayOrders after cancelling order', () => {
      fixture.detectChanges();
      const initialDisplayCount = component.displayOrders.length;
      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      const cancelledOrder = { ...pendingOrder, status: 'CANCELLED' as OrderStatus };
      orderServiceSpy.cancelOrder.and.returnValue(of(cancelledOrder));
      spyOn(window, 'confirm').and.returnValue(true);

      component.cancelOrder(pendingOrder);

      const updatedDisplayOrder = component.displayOrders.find((o) => o.id === pendingOrder.id);
      expect(updatedDisplayOrder?.status).toBe('CANCELLED');
      expect(component.displayOrders.length).toBe(initialDisplayCount);
    });

    it('should update stats after cancelling order', () => {
      fixture.detectChanges();
      const initialInProgress = component.buyerStats.inProgress;
      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      const cancelledOrder = { ...pendingOrder, status: 'CANCELLED' as OrderStatus };
      orderServiceSpy.cancelOrder.and.returnValue(of(cancelledOrder));
      spyOn(window, 'confirm').and.returnValue(true);

      component.cancelOrder(pendingOrder);

      expect(component.buyerStats.inProgress).toBe(initialInProgress - 1);
    });

    it('should not cancel if user declines confirmation', () => {
      fixture.detectChanges();
      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      spyOn(window, 'confirm').and.returnValue(false);
      component.cancelOrder(pendingOrder);
      expect(orderServiceSpy.cancelOrder).not.toHaveBeenCalled();
    });

    it('should not cancel order without id', () => {
      fixture.detectChanges();
      const orderWithoutId: Order = {
        ...createBuyerMockOrders()[2],
        id: undefined,
      };
      component.cancelOrder(orderWithoutId);
      expect(orderServiceSpy.cancelOrder).not.toHaveBeenCalled();
    });

    it('should handle error when cancelling order fails', () => {
      fixture.detectChanges();
      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      orderServiceSpy.cancelOrder.and.returnValue(throwError(() => new Error('Cancel failed')));
      spyOn(window, 'confirm').and.returnValue(true);
      spyOn(window, 'alert');
      component.cancelOrder(pendingOrder);
      expect(orderServiceSpy.cancelOrder).toHaveBeenCalledWith(pendingOrder.id!);
      expect(window.alert).toHaveBeenCalledWith('Failed to cancel order. Please try again.');
    });

    it('should handle error when loading orders', () => {
      orderServiceSpy.getOrders.and.returnValue(throwError(() => new Error('Network error')));
      fixture.detectChanges();
      expect(component.error).toBe('Failed to load orders. Please try again.');
      expect(component.loading).toBeFalse();
    });

    it('should show empty state when no orders', () => {
      orderServiceSpy.getOrders.and.returnValue(of([]));
      fixture.detectChanges();
      expect(component.orders.length).toBe(0);
      const compiled = fixture.nativeElement;
      expect(compiled.querySelector('.empty-state')).toBeTruthy();
    });
  });

  describe('Seller Mode', () => {
    beforeEach(() => {
      authServiceSpy.isSeller.and.returnValue(true);
      orderServiceSpy.getSellerOrders.and.returnValue(of(createSellerMockOrders()));
      fixture = TestBed.createComponent(OrderListComponent);
      component = fixture.componentInstance;
    });

    it('should create', () => {
      fixture.detectChanges();
      expect(component).toBeTruthy();
      expect(component.isSeller).toBeTrue();
    });

    it('should load seller orders on init', () => {
      fixture.detectChanges();
      expect(orderServiceSpy.getSellerOrders).toHaveBeenCalled();
      expect(orderServiceSpy.getOrders).not.toHaveBeenCalled();
      expect(component.orders.length).toBe(4);
    });

    it('should calculate seller stats correctly', () => {
      fixture.detectChanges();
      expect(component.sellerStats.totalOrders).toBe(4);
      expect(component.sellerStats.pending).toBe(1);
      expect(component.sellerStats.shipped).toBe(1);
      expect(component.sellerStats.delivered).toBe(1);
      expect(component.sellerStats.totalRevenue).toBe(155);
    });

    it('should handle empty orders', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of([]));
      fixture.detectChanges();
      expect(component.sellerStats.totalOrders).toBe(0);
      expect(component.sellerStats.totalRevenue).toBe(0);
    });

    it('should filter by status', () => {
      fixture.detectChanges();
      component.statusFilter = 'PENDING';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(1);
      expect(component.displayOrders[0].status).toBe('PENDING');
    });

    it('should filter by search query (order ID)', () => {
      fixture.detectChanges();
      component.searchQuery = 'order-1';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(1);
      expect(component.displayOrders[0].id).toBe('order-1');
    });

    it('should filter by search query (product name)', () => {
      fixture.detectChanges();
      component.searchQuery = 'lipstick';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(1);
      expect(component.displayOrders[0].items[0].productName).toBe('Lipstick');
    });

    it('should filter by search query (customer name)', () => {
      fixture.detectChanges();
      component.searchQuery = 'jane';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(1);
      expect(component.displayOrders[0].shippingAddress.fullName).toBe('Jane Doe');
    });

    it('should combine status and search filters', () => {
      fixture.detectChanges();
      component.statusFilter = 'DELIVERED';
      component.searchQuery = 'mascara';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(1);
    });

    it('should return empty array when no match', () => {
      fixture.detectChanges();
      component.searchQuery = 'nonexistent';
      component.applyFilters();
      expect(component.displayOrders.length).toBe(0);
    });

    it('should clear all filters', () => {
      fixture.detectChanges();
      const initialOrdersCount = component.orders.length;
      component.statusFilter = 'PENDING';
      component.searchQuery = 'test';
      component.applyFilters();
      const filteredCount = component.displayOrders.length;
      
      // Reset spy calls to verify clearFilters doesn't reload
      orderServiceSpy.getSellerOrders.calls.reset();
      component.clearFilters();
      
      expect(component.statusFilter).toBe('');
      expect(component.searchQuery).toBe('');
      // Should show all orders after clearing filters
      expect(component.displayOrders.length).toBe(initialOrdersCount);
      // Should NOT reload orders from server
      expect(orderServiceSpy.getSellerOrders).not.toHaveBeenCalled();
    });

    it('should update order status successfully', () => {
      fixture.detectChanges();
      const order = component.orders[0];
      const updatedOrder = { ...order, status: 'READY_FOR_DELIVERY' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));
      component.updateOrderStatus(order, 'READY_FOR_DELIVERY');
      expect(orderServiceSpy.updateOrderStatus).toHaveBeenCalledWith('order-1', 'READY_FOR_DELIVERY');
      expect(component.successMessage).toContain('Ready for Delivery');
    });

    it('should update displayOrders after status change', () => {
      fixture.detectChanges();
      const order = component.orders[0];
      const initialStatus = order.status;
      const updatedOrder = { ...order, status: 'SHIPPED' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));
      
      component.updateOrderStatus(order, 'SHIPPED');
      
      const updatedDisplayOrder = component.displayOrders.find((o) => o.id === order.id);
      expect(updatedDisplayOrder?.status).toBe('SHIPPED');
      expect(updatedDisplayOrder?.status).not.toBe(initialStatus);
    });

    it('should update stats after status change', () => {
      fixture.detectChanges();
      const order = component.orders.find((o) => o.status === 'PENDING')!;
      const initialPending = component.sellerStats.pending;
      const initialShipped = component.sellerStats.shipped;
      const updatedOrder = { ...order, status: 'SHIPPED' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));
      
      component.updateOrderStatus(order, 'SHIPPED');
      
      expect(component.sellerStats.pending).toBe(initialPending - 1);
      expect(component.sellerStats.shipped).toBe(initialShipped + 1);
    });

    it('should apply filters after status change when filters are active', () => {
      fixture.detectChanges();
      const order = component.orders.find((o) => o.status === 'PENDING')!;
      component.statusFilter = 'PENDING';
      component.applyFilters();
      const initialFilteredCount = component.displayOrders.length;
      
      const updatedOrder = { ...order, status: 'SHIPPED' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));
      
      component.updateOrderStatus(order, 'SHIPPED');
      
      // Order should no longer appear in filtered results
      expect(component.displayOrders.length).toBe(initialFilteredCount - 1);
      expect(component.displayOrders.find((o) => o.id === order.id)).toBeUndefined();
    });

    it('should handle error when updating status', () => {
      fixture.detectChanges();
      orderServiceSpy.updateOrderStatus.and.returnValue(throwError(() => new Error('Update failed')));
      component.updateOrderStatus(component.orders[0], 'READY_FOR_DELIVERY');
      expect(component.error).toBe('Failed to update order status. Please try again.');
    });

    it('should not update if order has no ID', () => {
      fixture.detectChanges();
      const orderWithoutId = { ...component.orders[0], id: undefined };
      component.updateOrderStatus(orderWithoutId, 'READY_FOR_DELIVERY');
      expect(orderServiceSpy.updateOrderStatus).not.toHaveBeenCalled();
    });

    it('should not show success message when order is not found in local state', () => {
      fixture.detectChanges();
      const orderNotInList: Order = {
        id: 'non-existent-order',
        userId: 'user-1',
        items: [],
        status: 'PENDING',
        totalAmount: 0,
        shippingAddress: {
          fullName: 'Test',
          address: 'Test',
          city: 'Test',
          phone: '123',
        },
        createdAt: new Date(),
      };
      const updatedOrder = { ...orderNotInList, status: 'SHIPPED' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));
      orderServiceSpy.getSellerOrders.and.returnValue(of(component.orders));
      
      component.updateOrderStatus(orderNotInList, 'SHIPPED');
      
      // Success message should not be set when order is not found
      expect(component.successMessage).toBe('');
      // Should reload orders to sync state
      expect(orderServiceSpy.getSellerOrders).toHaveBeenCalled();
    });

    it('should handle error when loading orders', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(throwError(() => new Error('Network error')));
      fixture.detectChanges();
      expect(component.error).toBe('Failed to load orders. Please try again.');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Utility Methods', () => {
    beforeEach(() => {
      authServiceSpy.isSeller.and.returnValue(false);
      orderServiceSpy.getOrders.and.returnValue(of(createBuyerMockOrders()));
      fixture = TestBed.createComponent(OrderListComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    describe('Status handling', () => {
      it('should return correct status class', () => {
        expect(component.getStatusClass('PENDING')).toBe('status-pending');
        expect(component.getStatusClass('DELIVERED')).toBe('status-delivered');
        expect(component.getStatusClass('SHIPPED')).toBe('status-shipped');
        expect(component.getStatusClass('CANCELLED')).toBe('status-cancelled');
        expect(component.getStatusClass('READY_FOR_DELIVERY')).toBe('status-ready');
      });

      it('should return default status class for unknown status', () => {
        const unknownStatus = 'UNKNOWN' as OrderStatus;
        expect(component.getStatusClass(unknownStatus)).toBe('status-pending');
      });

      it('should return correct status labels for buyer', () => {
        expect(component.getStatusLabel('PENDING')).toBe('Processing');
        expect(component.getStatusLabel('DELIVERED')).toBe('Delivered');
        expect(component.getStatusLabel('SHIPPED')).toBe('Shipped');
        expect(component.getStatusLabel('CANCELLED')).toBe('Cancelled');
        expect(component.getStatusLabel('READY_FOR_DELIVERY')).toBe('Ready');
      });

      it('should return correct status labels for seller', () => {
        component.isSeller = true;
        expect(component.getStatusLabel('PENDING')).toBe('Pending');
        expect(component.getStatusLabel('READY_FOR_DELIVERY')).toBe('Ready for Delivery');
      });

      it('should return status string for unknown status label', () => {
        const unknownStatus = 'UNKNOWN' as OrderStatus;
        expect(component.getStatusLabel(unknownStatus)).toBe('UNKNOWN');
      });
    });

    describe('Date formatting', () => {
      it('should format Date object correctly', () => {
        const formatted = component.formatDate(new Date('2024-01-25'));
        expect(formatted).toContain('January');
        expect(formatted).toContain('25');
        expect(formatted).toContain('2024');
      });

      it('should format date string correctly', () => {
        const formatted = component.formatDate('2024-01-25T12:00:00Z');
        expect(formatted).toContain('January');
        expect(formatted).toContain('25');
        expect(formatted).toContain('2024');
      });

      it('should return empty string for undefined date (buyer)', () => {
        component.isSeller = false;
        expect(component.formatDate(undefined)).toBe('');
      });

      it('should return N/A for undefined date (seller)', () => {
        component.isSeller = true;
        expect(component.formatDate(undefined)).toBe('N/A');
      });
    });

    describe('Order number generation', () => {
      it('should generate order number with year and ID suffix', () => {
        const orderNumber = component.getOrderNumber(component.orders[0]);
        expect(orderNumber).toContain('ORD-2024-');
        expect(orderNumber).toContain('001');
      });

      it('should use current year when createdAt is undefined (buyer)', () => {
        component.isSeller = false;
        const orderWithoutDate: Order = {
          ...createBuyerMockOrders()[0],
          createdAt: undefined,
        };
        const orderNumber = component.getOrderNumber(orderWithoutDate);
        const currentYear = new Date().getFullYear();
        expect(orderNumber).toContain(`ORD-${currentYear}-`);
      });

      it('should return last 8 characters when createdAt is undefined (seller)', () => {
        component.isSeller = true;
        const orderWithoutDate: Order = {
          ...createBuyerMockOrders()[0],
          id: 'order-1234567890abcdef',
          createdAt: undefined,
        };
        const orderNumber = component.getOrderNumber(orderWithoutDate);
        // Should return last 8 characters, not formatted number
        expect(orderNumber).toBe('90abcdef');
        expect(orderNumber).not.toContain('ORD-');
      });

      it('should return full ID if shorter than 8 characters when createdAt is undefined (seller)', () => {
        component.isSeller = true;
        const orderWithoutDate: Order = {
          ...createBuyerMockOrders()[0],
          id: 'abc123',
          createdAt: undefined,
        };
        const orderNumber = component.getOrderNumber(orderWithoutDate);
        // Should return full ID if shorter than 8 characters
        expect(orderNumber).toBe('abc123');
      });

      it('should return empty string for order without ID (buyer)', () => {
        component.isSeller = false;
        const order: Order = {
          ...createBuyerMockOrders()[0],
          id: undefined,
        };
        expect(component.getOrderNumber(order)).toBe('');
      });

      it('should return N/A for order without ID (seller)', () => {
        component.isSeller = true;
        const order: Order = {
          ...createBuyerMockOrders()[0],
          id: undefined,
        };
        expect(component.getOrderNumber(order)).toBe('N/A');
      });
    });

    describe('Product images', () => {
      it('should load product images for order items', () => {
        const mockMedia = [{
          id: 'media-1',
          productId: 'prod-1',
          imagePath: '/images/prod-1.jpg',
          fileName: 'prod-1.jpg',
          contentType: 'image/jpeg',
          fileSize: 1024,
        }];
        // Reset spy calls from initialization
        mediaServiceSpy.getMediaByProduct.calls.reset();
        mediaServiceSpy.getMediaFile.calls.reset();
        mediaServiceSpy.getMediaByProduct.and.returnValue(of(mockMedia));
        mediaServiceSpy.getMediaFile.and.returnValue('http://example.com/image.jpg');
        // Manually trigger image loading since component is already initialized
        component.loadProductImages();
        expect(mediaServiceSpy.getMediaByProduct).toHaveBeenCalledWith('prod-1');
        expect(mediaServiceSpy.getMediaFile).toHaveBeenCalledWith('media-1');
      });

      it('should not set image when media array is empty', () => {
        // Reset spy calls from initialization
        mediaServiceSpy.getMediaByProduct.calls.reset();
        mediaServiceSpy.getMediaFile.calls.reset();
        mediaServiceSpy.getMediaByProduct.and.returnValue(of([]));
        // Manually trigger image loading since component is already initialized
        component.loadProductImages();
        expect(mediaServiceSpy.getMediaFile).not.toHaveBeenCalled();
      });

      it('should handle media loading errors silently', () => {
        // Reset spy calls from initialization
        mediaServiceSpy.getMediaByProduct.calls.reset();
        mediaServiceSpy.getMediaFile.calls.reset();
        mediaServiceSpy.getMediaByProduct.and.returnValue(
          throwError(() => new Error('Media not found'))
        );
        // Manually trigger image loading since component is already initialized
        component.loadProductImages();
        expect(component.productImages.size).toBe(0);
      });

      it('should return product image from map', () => {
        component.productImages.set('prod-1', 'http://example.com/image.jpg');
        expect(component.productImages.get('prod-1')).toBe('http://example.com/image.jpg');
      });

      it('should return undefined for unknown product', () => {
        expect(component.productImages.get('unknown-prod')).toBeUndefined();
      });
    });
  });
});
