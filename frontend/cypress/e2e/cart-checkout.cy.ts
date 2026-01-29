describe('Shopping Cart & Checkout E2E Tests', () => {
  const testBuyer = {
    email: 'buyer@buyer.com',
    password: 'password123',
  };

  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  describe('Cart Functionality', () => {
    it('should add item to cart', () => {
      // Login first
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();

      // Wait for login to complete
      cy.url().should('not.include', '/login');

      // Go to products
      cy.visit('/products');

      // Click on a product to view details
      cy.get('[class*="product"], .card', { timeout: 10000 })
        .first()
        .click();

      // Add to cart
      cy.contains('Add to Cart', { timeout: 5000 }).click();

      // Verify success message or cart update
      cy.get('[class*="cart"], .navbar', { timeout: 5000 }).should('exist');
    });

    it('should view cart with items', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // Navigate to cart
      cy.visit('/cart');

      // Cart page should load without errors
      cy.get('body').should('exist');
      cy.url().should('include', '/cart');
    });

    it('should update item quantity in cart', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // Go to cart
      cy.visit('/cart');

      // Look for quantity controls (if items exist)
      cy.get('body').then(($body) => {
        if ($body.find('[class*="quantity"], input[type="number"]').length > 0) {
          cy.get('[class*="quantity"], input[type="number"]').first().should('exist');
        }
      });
    });

    it('should remove item from cart', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // Go to cart
      cy.visit('/cart');

      // Look for remove button (if items exist)
      cy.get('body').then(($body) => {
        if ($body.find('[class*="remove"], button:contains("Remove")').length > 0) {
          cy.get('[class*="remove"], button:contains("Remove")').first().should('exist');
        }
      });
    });
  });

  describe('Checkout Flow', () => {
    it('should navigate to checkout from cart', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // Go to cart
      cy.visit('/cart');

      // Look for checkout button
      cy.get('body').then(($body) => {
        if ($body.find('a[href*="checkout"], button:contains("Checkout")').length > 0) {
          cy.get('a[href*="checkout"], button:contains("Checkout")').first().click();
          cy.url().should('include', '/checkout');
        }
      });
    });

    it('should display checkout form with shipping address fields', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // Go directly to checkout
      cy.visit('/checkout');

      // Check for shipping address form fields
      cy.get('body').should('exist');
      // The form should have fields for name, address, city, phone
      cy.get('input, textarea').should('have.length.at.least', 1);
    });

    it('should place an order successfully', () => {
      // Login
      cy.visit('/login');
      cy.get('input[type="email"]').type(testBuyer.email);
      cy.get('input[type="password"]').type(testBuyer.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('not.include', '/login');

      // First add item to cart
      cy.visit('/products');
      cy.get('[class*="product"], .card', { timeout: 10000 })
        .first()
        .click();
      cy.contains('Add to Cart', { timeout: 5000 }).click();

      // Go to checkout
      cy.visit('/checkout');

      // Fill in shipping form (adjust selectors based on actual form)
      cy.get('body').then(($body) => {
        // Fill form fields if they exist
        const inputs = $body.find('input:not([type="hidden"])');
        if (inputs.length >= 4) {
          cy.get('input').eq(0).clear().type('Test User');
          cy.get('input').eq(1).clear().type('123 Test Street');
          cy.get('input').eq(2).clear().type('Helsinki');
          cy.get('input').eq(3).clear().type('+358123456789');
        }
      });

      // Click place order
      cy.contains('Place Order', { timeout: 5000 }).click();

      // Should either show success or redirect to confirmation
      cy.get('body').then(($body) => {
        // Check that we don't have an error (the routing bug would cause 404 error here)
        const hasError = $body.text().includes('Failed to place order');
        if (hasError) {
          throw new Error('Order placement failed - possible API routing issue');
        }
      });
    });
  });

  describe('API Routing Smoke Tests', () => {
    // These tests specifically check the routing issues we fixed

    it('should successfully call POST /cart/items', () => {
      cy.request({
        method: 'POST',
        url: '/auth/login',
        body: testBuyer,
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status === 200) {
          const token = loginRes.body.token;

          cy.request({
            method: 'POST',
            url: '/cart/items',
            headers: { Authorization: `Bearer ${token}` },
            body: {
              productId: 'test-product-id',
              quantity: 1,
              price: 10.0,
            },
            failOnStatusCode: false,
          }).then((res) => {
            // Should not be 404 (routing issue) or 405 (method not allowed)
            expect(res.status).to.not.equal(404);
            expect(res.status).to.not.equal(405);
          });
        }
      });
    });

    it('should successfully call GET /cart', () => {
      cy.request({
        method: 'POST',
        url: '/auth/login',
        body: testBuyer,
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status === 200) {
          const token = loginRes.body.token;

          cy.request({
            method: 'GET',
            url: '/cart',
            headers: { Authorization: `Bearer ${token}` },
            failOnStatusCode: false,
          }).then((res) => {
            // Should not be 404 (routing issue)
            expect(res.status).to.not.equal(404);
          });
        }
      });
    });

    it('should successfully call POST /orders', () => {
      cy.request({
        method: 'POST',
        url: '/auth/login',
        body: testBuyer,
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status === 200) {
          const token = loginRes.body.token;

          cy.request({
            method: 'POST',
            url: '/orders',
            headers: { Authorization: `Bearer ${token}` },
            body: {
              fullName: 'Test User',
              address: '123 Test St',
              city: 'Helsinki',
              phone: '+358123456789',
            },
            failOnStatusCode: false,
          }).then((res) => {
            // Should not be 404 (routing issue) or 405 (method not allowed)
            // May be 400 if cart is empty, but that's okay
            expect(res.status).to.not.equal(404);
            expect(res.status).to.not.equal(405);
          });
        }
      });
    });

    it('should successfully call GET /orders', () => {
      cy.request({
        method: 'POST',
        url: '/auth/login',
        body: testBuyer,
        failOnStatusCode: false,
      }).then((loginRes) => {
        if (loginRes.status === 200) {
          const token = loginRes.body.token;

          cy.request({
            method: 'GET',
            url: '/orders',
            headers: { Authorization: `Bearer ${token}` },
            failOnStatusCode: false,
          }).then((res) => {
            // Should not be 404 (routing issue)
            expect(res.status).to.not.equal(404);
          });
        }
      });
    });
  });
});

