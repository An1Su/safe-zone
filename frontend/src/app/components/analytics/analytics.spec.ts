import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Analytics } from './analytics';
import { OrderService } from '../../services/order.service';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { Order, OrderStatus, ShippingAddress } from '../../models/order.model';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

describe('Analytics', () => {
  let component: Analytics;
  let fixture: ComponentFixture<Analytics>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockShippingAddress: ShippingAddress = {
    fullName: 'John Doe',
    address: '123 Main St',
    city: 'New York',
    phone: '555-1234',
  };

  const mockBuyerOrders: Order[] = [
    {
      id: 'order1',
      userId: 'user1',
      items: [
        { productId: 'p1', productName: 'Product A', sellerId: 'seller1', quantity: 3, price: 10 },
        { productId: 'p2', productName: 'Product B', sellerId: 'seller1', quantity: 2, price: 20 },
      ],
      totalAmount: 70,
      status: 'DELIVERED' as OrderStatus,
      shippingAddress: mockShippingAddress,
      createdAt: new Date('2026-01-15T10:00:00Z'),
    },
    {
      id: 'order2',
      userId: 'user1',
      items: [
        { productId: 'p1', productName: 'Product A', sellerId: 'seller1', quantity: 5, price: 10 },
      ],
      totalAmount: 50,
      status: 'DELIVERED' as OrderStatus,
      shippingAddress: mockShippingAddress,
      createdAt: new Date('2026-01-16T10:00:00Z'),
    },
    {
      id: 'order3',
      userId: 'user1',
      items: [
        { productId: 'p3', productName: 'Product C', sellerId: 'seller1', quantity: 1, price: 100 },
      ],
      totalAmount: 100,
      status: 'PENDING' as OrderStatus, // Should be filtered out
      shippingAddress: mockShippingAddress,
      createdAt: new Date('2026-01-17T10:00:00Z'),
    },
  ];

  const mockSellerOrders: Order[] = [
    {
      id: 'order1',
      userId: 'buyer1',
      items: [
        { productId: 'p1', productName: 'Seller Product A', sellerId: 'seller1', quantity: 10, price: 15 },
        { productId: 'p2', productName: 'Seller Product B', sellerId: 'seller1', quantity: 5, price: 25 },
      ],
      totalAmount: 275,
      status: 'DELIVERED' as OrderStatus,
      shippingAddress: mockShippingAddress,
      createdAt: new Date('2026-01-15T10:00:00Z'),
    },
    {
      id: 'order2',
      userId: 'buyer2',
      items: [
        { productId: 'p1', productName: 'Seller Product A', sellerId: 'seller1', quantity: 8, price: 15 },
      ],
      totalAmount: 120,
      status: 'DELIVERED' as OrderStatus,
      shippingAddress: mockShippingAddress,
      createdAt: new Date('2026-01-16T10:00:00Z'),
    },
  ];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['getOrders', 'getSellerOrders']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['isClient', 'isSeller']);

    // Default: not logged in
    authServiceSpy.isClient.and.returnValue(false);
    authServiceSpy.isSeller.and.returnValue(false);
    orderServiceSpy.getOrders.and.returnValue(of([]));
    orderServiceSpy.getSellerOrders.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Analytics],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        provideCharts(withDefaultRegisterables()),
        provideRouter([]),
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Analytics);
    component = fixture.componentInstance;
  }

  describe('Component creation', () => {
    it('should create', () => {
      createComponent();
      expect(component).toBeTruthy();
    });

    it('should have default values', () => {
      createComponent();
      expect(component.loading).toBeTrue();
      expect(component.error).toBe('');
      expect(component.isBuyer).toBeFalse();
      expect(component.isSeller).toBeFalse();
    });
  });

  describe('Unauthorized access', () => {
    it('should set error for unauthorized users', () => {
      authServiceSpy.isClient.and.returnValue(false);
      authServiceSpy.isSeller.and.returnValue(false);

      createComponent();
      fixture.detectChanges();

      expect(component.error).toBe('Unauthorized access');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Buyer Analytics', () => {
    beforeEach(() => {
      authServiceSpy.isClient.and.returnValue(true);
      authServiceSpy.isSeller.and.returnValue(false);
    });

    it('should load buyer analytics on init', () => {
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.isBuyer).toBeTrue();
      expect(orderServiceSpy.getOrders).toHaveBeenCalled();
      expect(component.loading).toBeFalse();
    });

    it('should calculate buyer stats correctly', () => {
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      
      createComponent();
      fixture.detectChanges();

      // Only DELIVERED orders: order1 (70) + order2 (50) = 120
      expect(component.buyerStats.totalSpent).toBe(120);
      // Product A: 3 + 5 = 8, Product B: 2
      expect(component.buyerStats.mostBoughtProducts.length).toBe(2);
      expect(component.buyerStats.mostBoughtProducts[0].name).toBe('Product A');
      expect(component.buyerStats.mostBoughtProducts[0].quantity).toBe(8);
    });

    it('should populate buyer chart data', () => {
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.buyerProductChartData.labels?.length).toBe(2);
      expect(component.buyerProductChartData.datasets[0].data.length).toBe(2);
    });

    it('should handle empty buyer orders', () => {
      orderServiceSpy.getOrders.and.returnValue(of([]));
      
      createComponent();
      fixture.detectChanges();

      expect(component.buyerStats.totalSpent).toBe(0);
      expect(component.buyerStats.mostBoughtProducts.length).toBe(0);
    });

    it('should handle buyer analytics error', () => {
      orderServiceSpy.getOrders.and.returnValue(throwError(() => new Error('Network error')));
      
      createComponent();
      fixture.detectChanges();

      expect(component.error).toBe('Failed to load analytics');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Seller Analytics', () => {
    beforeEach(() => {
      authServiceSpy.isClient.and.returnValue(false);
      authServiceSpy.isSeller.and.returnValue(true);
    });

    it('should load seller analytics on init', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of(mockSellerOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.isSeller).toBeTrue();
      expect(orderServiceSpy.getSellerOrders).toHaveBeenCalled();
      expect(component.loading).toBeFalse();
    });

    it('should calculate seller stats correctly', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of(mockSellerOrders));
      
      createComponent();
      fixture.detectChanges();

      // Seller Product A: 10 + 8 = 18 units, (10*15) + (8*15) = 270 revenue
      // Seller Product B: 5 units, 5*25 = 125 revenue
      expect(component.sellerStats.totalUnitsSold).toBe(23);
      expect(component.sellerStats.revenue).toBe(395);
      expect(component.sellerStats.bestSellingProducts.length).toBe(2);
      expect(component.sellerStats.bestSellingProducts[0].name).toBe('Seller Product A');
      expect(component.sellerStats.bestSellingProducts[0].unitsSold).toBe(18);
    });

    it('should populate seller chart data', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of(mockSellerOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.sellerProductChartData.labels?.length).toBe(2);
      expect(component.sellerProductChartData.datasets[0].data.length).toBe(2);
      expect(component.sellerRevenueChartData.labels?.length).toBe(2);
      expect(component.sellerRevenueChartData.datasets[0].data.length).toBe(2);
    });

    it('should handle empty seller orders', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of([]));
      
      createComponent();
      fixture.detectChanges();

      expect(component.sellerStats.revenue).toBe(0);
      expect(component.sellerStats.totalUnitsSold).toBe(0);
      expect(component.sellerStats.bestSellingProducts.length).toBe(0);
    });

    it('should handle seller analytics error', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(throwError(() => new Error('Network error')));
      
      createComponent();
      fixture.detectChanges();

      expect(component.error).toBe('Failed to load analytics');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Reload functionality', () => {
    it('should reload buyer analytics', () => {
      authServiceSpy.isClient.and.returnValue(true);
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      
      createComponent();
      fixture.detectChanges();

      // Simulate reload
      component.reload();

      expect(component.loading).toBeFalse(); // After reload completes
      expect(orderServiceSpy.getOrders).toHaveBeenCalledTimes(2);
    });

    it('should reload seller analytics', () => {
      authServiceSpy.isSeller.and.returnValue(true);
      orderServiceSpy.getSellerOrders.and.returnValue(of(mockSellerOrders));
      
      createComponent();
      fixture.detectChanges();

      // Simulate reload
      component.reload();

      expect(component.loading).toBeFalse();
      expect(orderServiceSpy.getSellerOrders).toHaveBeenCalledTimes(2);
    });

    it('should reset error on reload', () => {
      authServiceSpy.isClient.and.returnValue(true);
      orderServiceSpy.getOrders.and.returnValue(throwError(() => new Error('Error')));
      
      createComponent();
      fixture.detectChanges();

      expect(component.error).toBe('Failed to load analytics');

      // Now make it succeed
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      component.reload();

      expect(component.error).toBe('');
    });
  });

  describe('Chart options', () => {
    it('should have proper chart options configured', () => {
      createComponent();
      
      expect(component.chartOptions.responsive).toBeTrue();
      expect(component.chartOptions.maintainAspectRatio).toBeFalse();
      expect(component.chartOptions.plugins?.legend?.display).toBeTrue();
      expect(component.chartOptions.plugins?.tooltip?.enabled).toBeTrue();
      expect(component.chartOptions.scales).toBeDefined();
      expect(component.chartOptions.scales?.['y']).toBeDefined();
    });
  });

  describe('Filter valid orders', () => {
    it('should only include DELIVERED orders for buyer', () => {
      authServiceSpy.isClient.and.returnValue(true);
      orderServiceSpy.getOrders.and.returnValue(of(mockBuyerOrders));
      
      createComponent();
      fixture.detectChanges();

      // mockBuyerOrders has 3 orders, but only 2 are DELIVERED
      // Total from DELIVERED: 70 + 50 = 120
      expect(component.buyerStats.totalSpent).toBe(120);
    });

    it('should only include DELIVERED orders for seller', () => {
      authServiceSpy.isSeller.and.returnValue(true);
      
      const ordersWithPending: Order[] = [
        ...mockSellerOrders,
        {
          id: 'order3',
          userId: 'buyer3',
          items: [{ productId: 'p1', productName: 'Seller Product A', sellerId: 'seller1', quantity: 100, price: 15 }],
          totalAmount: 1500,
          status: 'PENDING' as OrderStatus,
          shippingAddress: mockShippingAddress,
          createdAt: new Date('2026-01-17T10:00:00Z'),
        },
      ];
      
      orderServiceSpy.getSellerOrders.and.returnValue(of(ordersWithPending));
      
      createComponent();
      fixture.detectChanges();

      // PENDING order should be excluded, so total units is still 23
      expect(component.sellerStats.totalUnitsSold).toBe(23);
    });
  });

  describe('Top 5 products limit', () => {
    it('should limit buyer products to top 5', () => {
      authServiceSpy.isClient.and.returnValue(true);
      
      const manyProductsOrders: Order[] = [{
        id: 'order1',
        userId: 'user1',
        items: [
          { productId: 'p1', productName: 'Product 1', sellerId: 's1', quantity: 10, price: 10 },
          { productId: 'p2', productName: 'Product 2', sellerId: 's1', quantity: 9, price: 10 },
          { productId: 'p3', productName: 'Product 3', sellerId: 's1', quantity: 8, price: 10 },
          { productId: 'p4', productName: 'Product 4', sellerId: 's1', quantity: 7, price: 10 },
          { productId: 'p5', productName: 'Product 5', sellerId: 's1', quantity: 6, price: 10 },
          { productId: 'p6', productName: 'Product 6', sellerId: 's1', quantity: 5, price: 10 },
          { productId: 'p7', productName: 'Product 7', sellerId: 's1', quantity: 4, price: 10 },
        ],
        totalAmount: 490,
        status: 'DELIVERED' as OrderStatus,
        shippingAddress: mockShippingAddress,
        createdAt: new Date('2026-01-15T10:00:00Z'),
      }];
      
      orderServiceSpy.getOrders.and.returnValue(of(manyProductsOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.buyerStats.mostBoughtProducts.length).toBe(5);
      expect(component.buyerStats.mostBoughtProducts[0].quantity).toBe(10);
      expect(component.buyerStats.mostBoughtProducts[4].quantity).toBe(6);
    });

    it('should limit seller products to top 5', () => {
      authServiceSpy.isSeller.and.returnValue(true);
      
      const manyProductsOrders: Order[] = [{
        id: 'order1',
        userId: 'buyer1',
        items: [
          { productId: 'p1', productName: 'Product 1', sellerId: 's1', quantity: 10, price: 10 },
          { productId: 'p2', productName: 'Product 2', sellerId: 's1', quantity: 9, price: 10 },
          { productId: 'p3', productName: 'Product 3', sellerId: 's1', quantity: 8, price: 10 },
          { productId: 'p4', productName: 'Product 4', sellerId: 's1', quantity: 7, price: 10 },
          { productId: 'p5', productName: 'Product 5', sellerId: 's1', quantity: 6, price: 10 },
          { productId: 'p6', productName: 'Product 6', sellerId: 's1', quantity: 5, price: 10 },
          { productId: 'p7', productName: 'Product 7', sellerId: 's1', quantity: 4, price: 10 },
        ],
        totalAmount: 490,
        status: 'DELIVERED' as OrderStatus,
        shippingAddress: mockShippingAddress,
        createdAt: new Date('2026-01-15T10:00:00Z'),
      }];
      
      orderServiceSpy.getSellerOrders.and.returnValue(of(manyProductsOrders));
      
      createComponent();
      fixture.detectChanges();

      expect(component.sellerStats.bestSellingProducts.length).toBe(5);
    });
  });
});
