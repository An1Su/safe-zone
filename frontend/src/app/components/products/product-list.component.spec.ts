import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { ProductListComponent } from './product-list.component';
import { ProductService, ProductSearchResult } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { AuthService } from '../../services/auth.service';
import { Product } from '../../models/ecommerce.model';

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let productService: jasmine.SpyObj<ProductService>;
  let cartService: jasmine.SpyObj<CartService>;
  let mediaService: jasmine.SpyObj<MediaService>;
  let authService: jasmine.SpyObj<AuthService>;

  const mockProducts: Product[] = [
    {
      id: '1',
      name: 'Face Cream',
      description: 'Moisturizing face cream',
      price: 29.99,
      stock: 10,
      category: 'Face',
    },
    {
      id: '2',
      name: 'Eye Shadow',
      description: 'Beautiful eye shadow palette',
      price: 49.99,
      stock: 5,
      category: 'Eyes',
    },
    {
      id: '3',
      name: 'Lipstick',
      description: 'Red lipstick',
      price: 19.99,
      stock: 15,
      category: 'Lips',
    },
  ];

  const mockSearchResult: ProductSearchResult = {
    products: mockProducts,
    total: 3,
    page: 1,
    totalPages: 1,
  };

  beforeEach(async () => {
    const productServiceSpy = jasmine.createSpyObj('ProductService', [
      'searchProducts',
      'getCategories',
    ]);
    const cartServiceSpy = jasmine.createSpyObj('CartService', ['addToCart']);
    const mediaServiceSpy = jasmine.createSpyObj('MediaService', [
      'getMediaByProduct',
      'getMediaFile',
    ]);
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'isLoggedIn',
      'getUserRole',
    ]);

    productServiceSpy.searchProducts.and.returnValue(of(mockSearchResult));
    productServiceSpy.getCategories.and.returnValue(['Face', 'Eyes', 'Lips']);
    mediaServiceSpy.getMediaByProduct.and.returnValue(of([]));
    authServiceSpy.isLoggedIn.and.returnValue(true);
    authServiceSpy.getUserRole.and.returnValue('client');

    await TestBed.configureTestingModule({
      imports: [
        ProductListComponent,
        HttpClientTestingModule,
        RouterTestingModule,
        FormsModule,
      ],
      providers: [
        { provide: ProductService, useValue: productServiceSpy },
        { provide: CartService, useValue: cartServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
    cartService = TestBed.inject(CartService) as jasmine.SpyObj<CartService>;
    mediaService = TestBed.inject(MediaService) as jasmine.SpyObj<MediaService>;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    fixture.detectChanges();
    expect(productService.searchProducts).toHaveBeenCalled();
    expect(component.products.length).toBe(3);
    expect(component.loading).toBeFalse();
  });

  it('should have correct initial filter values', () => {
    expect(component.searchQuery).toBe('');
    expect(component.selectedCategory).toBeNull();
    expect(component.minPrice).toBe(0);
    expect(component.priceRange).toBe(100);
    expect(component.sortBy).toBe('newest');
    expect(component.currentPage).toBe(1);
  });

  it('should have correct categories', () => {
    expect(component.categories).toEqual(['Face', 'Eyes', 'Lips']);
  });

  describe('onSearch', () => {
    it('should reset page and reload products', () => {
      component.currentPage = 3;
      component.searchQuery = 'lipstick';
      component.onSearch();
      
      expect(component.currentPage).toBe(1);
      expect(productService.searchProducts).toHaveBeenCalled();
    });
  });

  describe('onCategoryChange', () => {
    it('should set selected category', () => {
      fixture.detectChanges();
      component.onCategoryChange('Face');
      
      expect(component.selectedCategory).toBe('Face');
      expect(component.currentPage).toBe(1);
    });

    it('should toggle off category when clicking same category', () => {
      component.selectedCategory = 'Face';
      component.onCategoryChange('Face');
      
      expect(component.selectedCategory).toBeNull();
    });
  });

  describe('onSortChange', () => {
    it('should reset page and reload products', () => {
      fixture.detectChanges();
      component.currentPage = 2;
      component.sortBy = 'price_asc';
      component.onSortChange();
      
      expect(component.currentPage).toBe(1);
      expect(productService.searchProducts).toHaveBeenCalled();
    });
  });

  describe('clearFilters', () => {
    it('should reset all filters to defaults', () => {
      component.searchQuery = 'test';
      component.selectedCategory = 'Face';
      component.priceRange = 50;
      component.sortBy = 'price_desc';
      component.currentPage = 3;

      component.clearFilters();

      expect(component.searchQuery).toBe('');
      expect(component.selectedCategory).toBeNull();
      expect(component.priceRange).toBe(100);
      expect(component.sortBy).toBe('newest');
      expect(component.currentPage).toBe(1);
    });
  });

  describe('pagination', () => {
    it('should go to next page', () => {
      fixture.detectChanges();
      component.totalPages = 3;
      component.currentPage = 1;
      
      component.goToPage(2);
      
      expect(component.currentPage).toBe(2);
    });

    it('should not go beyond total pages', () => {
      fixture.detectChanges();
      component.totalPages = 3;
      component.currentPage = 3;
      
      component.goToPage(4);
      
      expect(component.currentPage).toBe(3);
    });

    it('should not go below page 1', () => {
      fixture.detectChanges();
      component.currentPage = 1;
      
      component.goToPage(0);
      
      expect(component.currentPage).toBe(1);
    });

    it('should generate correct page numbers', () => {
      component.currentPage = 3;
      component.totalPages = 10;
      
      const pages = component.getPageNumbers();
      
      expect(pages.length).toBeLessThanOrEqual(5);
      expect(pages).toContain(3);
    });
  });

  describe('view mode', () => {
    it('should default to grid view', () => {
      expect(component.viewMode).toBe('grid');
    });

    it('should switch to list view', () => {
      component.setViewMode('list');
      expect(component.viewMode).toBe('list');
    });

    it('should switch back to grid view', () => {
      component.viewMode = 'list';
      component.setViewMode('grid');
      expect(component.viewMode).toBe('grid');
    });
  });

  describe('isBuyer', () => {
    it('should return true for logged in client', () => {
      authService.isLoggedIn.and.returnValue(true);
      authService.getUserRole.and.returnValue('client');
      
      expect(component.isBuyer()).toBeTrue();
    });

    it('should return false for seller', () => {
      authService.isLoggedIn.and.returnValue(true);
      authService.getUserRole.and.returnValue('seller');
      
      expect(component.isBuyer()).toBeFalse();
    });

    it('should return false for logged out user', () => {
      authService.isLoggedIn.and.returnValue(false);
      
      expect(component.isBuyer()).toBeFalse();
    });
  });

  describe('addToCart', () => {
    it('should not add to cart if not logged in', () => {
      authService.isLoggedIn.and.returnValue(false);
      const event = new MouseEvent('click');
      spyOn(window, 'alert');

      component.addToCart(mockProducts[0], event);

      expect(cartService.addToCart).not.toHaveBeenCalled();
      expect(window.alert).toHaveBeenCalledWith('Please log in to add items to cart');
    });

    it('should call cart service for logged in buyer', () => {
      authService.isLoggedIn.and.returnValue(true);
      cartService.addToCart.and.returnValue(of({
        userId: '1',
        items: [],
        total: 0,
      }));
      const event = new MouseEvent('click');
      spyOn(event, 'stopPropagation');
      spyOn(event, 'preventDefault');
      spyOn(window, 'alert');

      component.addToCart(mockProducts[0], event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(event.preventDefault).toHaveBeenCalled();
      expect(cartService.addToCart).toHaveBeenCalled();
    });
  });

  describe('product images', () => {
    it('should return empty array when no media', () => {
      const urls = component.getProductImageUrls('1');
      expect(urls).toEqual([]);
    });

    it('should return null for first image when no media', () => {
      const url = component.getFirstProductImage('1');
      expect(url).toBeNull();
    });
  });
});

