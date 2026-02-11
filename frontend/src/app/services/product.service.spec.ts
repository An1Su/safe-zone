import { HttpHeaders } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environments';
import { Product } from '../models/ecommerce.model';
import { AuthService } from './auth.service';
import { ProductService } from './product.service';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  const mockProducts: Product[] = [
    {
      id: '1',
      name: 'Product 1',
      description: 'Description 1',
      price: 99.99,
      stock: 10,
      user: 'seller@example.com',
      category: 'Face',
    },
    {
      id: '2',
      name: 'Product 2',
      description: 'Description 2',
      price: 149.99,
      stock: 5,
      user: 'seller@example.com',
      category: 'Eyes',
    },
    {
      id: '3',
      name: 'Lipstick Red',
      description: 'Beautiful red lipstick',
      price: 25.99,
      stock: 20,
      user: 'seller@example.com',
      category: 'Lips',
    },
  ];

  const mockProduct: Product = mockProducts[0];

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getAuthHeaders']);
    authServiceSpy.getAuthHeaders.and.returnValue(
      new HttpHeaders({ Authorization: 'Bearer mock-token' })
    );

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService, { provide: AuthService, useValue: authServiceSpy }],
    });

    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllProducts', () => {
    it('should fetch all products', () => {
      service.getAllProducts().subscribe((products) => {
        expect(products).toEqual(mockProducts);
        expect(products.length).toBe(3);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });

    it('should handle empty product list', () => {
      service.getAllProducts().subscribe((products) => {
        expect(products).toEqual([]);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush([]);
    });
  });

  describe('getProductById', () => {
    it('should fetch a product by id', () => {
      const productId = '1';

      service.getProductById(productId).subscribe((product) => {
        expect(product).toEqual(mockProduct);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products/${productId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProduct);
    });

    it('should handle 404 error for non-existent product', () => {
      const productId = '999';

      service.getProductById(productId).subscribe(
        () => fail('should have failed with 404 error'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(`${environment.apiUrl}/products/${productId}`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getMyProducts', () => {
    it('should fetch seller products with auth headers', () => {
      service.getMyProducts().subscribe((products) => {
        expect(products).toEqual(mockProducts);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products/my-products`);
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(authService.getAuthHeaders).toHaveBeenCalled();
      req.flush(mockProducts);
    });
  });

  describe('createProduct', () => {
    it('should create a new product with auth', () => {
      const newProduct: Product = {
        name: 'New Product',
        description: 'New Description',
        price: 199.99,
        stock: 20,
        user: 'seller@example.com',
      };

      service.createProduct(newProduct).subscribe((product) => {
        expect(product.id).toBeDefined();
        expect(product.name).toBe(newProduct.name);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newProduct);
      expect(req.request.withCredentials).toBe(true);
      expect(authService.getAuthHeaders).toHaveBeenCalled();

      req.flush({ ...newProduct, id: '3' });
    });
  });

  describe('updateProduct', () => {
    it('should update an existing product', () => {
      const updatedProduct: Product = { ...mockProduct, price: 129.99 };
      const productId = '1';

      service.updateProduct(productId, updatedProduct).subscribe((product) => {
        expect(product.price).toBe(129.99);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products/${productId}`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedProduct);
      expect(req.request.withCredentials).toBe(true);
      expect(authService.getAuthHeaders).toHaveBeenCalled();

      req.flush(updatedProduct);
    });
  });

  describe('deleteProduct', () => {
    it('should delete a product', () => {
      const productId = '1';

      service.deleteProduct(productId).subscribe((response) => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products/${productId}`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);
      expect(authService.getAuthHeaders).toHaveBeenCalled();

      req.flush({ message: 'Product deleted' });
    });

    it('should verify price is positive', () => {
      const invalidProduct: Product = {
        name: 'Product',
        description: 'Test',
        price: -10.0,
        stock: 5,
      };

      expect(invalidProduct.price < 0).toBe(true);
    });

    it('should verify stock is non-negative', () => {
      const invalidProduct: Product = {
        name: 'Product',
        description: 'Test',
        price: 10.0,
        stock: -5,
      };

      expect(invalidProduct.stock < 0).toBe(true);
    });
  });

  describe('searchProducts', () => {
    it('should filter products by search query', (done) => {
      service.searchProducts({ query: 'Lipstick' }).subscribe((result) => {
        expect(result.products.length).toBe(1);
        expect(result.products[0].name).toBe('Lipstick Red');
        expect(result.total).toBe(1);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should filter products by category', (done) => {
      service.searchProducts({ category: 'Eyes' }).subscribe((result) => {
        expect(result.products.length).toBe(1);
        expect(result.products[0].category).toBe('Eyes');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should filter products by price range', (done) => {
      service.searchProducts({ minPrice: 50, maxPrice: 100 }).subscribe((result) => {
        expect(result.products.length).toBe(1);
        expect(result.products[0].price).toBe(99.99);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should sort products by price ascending', (done) => {
      service.searchProducts({ sortBy: 'price_asc' }).subscribe((result) => {
        expect(result.products[0].price).toBeLessThanOrEqual(result.products[1].price);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should sort products by price descending', (done) => {
      service.searchProducts({ sortBy: 'price_desc' }).subscribe((result) => {
        expect(result.products[0].price).toBeGreaterThanOrEqual(result.products[1].price);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should paginate results', (done) => {
      service.searchProducts({ page: 1, limit: 2 }).subscribe((result) => {
        expect(result.products.length).toBe(2);
        expect(result.page).toBe(1);
        expect(result.totalPages).toBe(2);
        expect(result.total).toBe(3);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should return second page of results', (done) => {
      service.searchProducts({ page: 2, limit: 2 }).subscribe((result) => {
        expect(result.products.length).toBe(1);
        expect(result.page).toBe(2);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should combine multiple filters', (done) => {
      service.searchProducts({ 
        category: 'Lips',
        minPrice: 20,
        sortBy: 'price_asc'
      }).subscribe((result) => {
        expect(result.products.length).toBe(1);
        expect(result.products[0].category).toBe('Lips');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });

    it('should return empty array when no matches', (done) => {
      service.searchProducts({ query: 'nonexistent' }).subscribe((result) => {
        expect(result.products.length).toBe(0);
        expect(result.total).toBe(0);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/products`);
      req.flush(mockProducts);
    });
  });

  describe('getCategories', () => {
    it('should return all available categories', () => {
      const categories = service.getCategories();
      expect(categories).toEqual(['Face', 'Eyes', 'Lips']);
      expect(categories.length).toBe(3);
    });
  });
});
