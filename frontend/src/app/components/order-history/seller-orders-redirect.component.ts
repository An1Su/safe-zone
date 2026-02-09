import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-seller-orders-redirect',
  standalone: true,
  template: '',
})
export class SellerOrdersRedirectComponent implements OnInit {
  constructor(private readonly router: Router) {}

  ngOnInit(): void {
    // Guard ensures only sellers can reach this component
    // Simply redirect to unified orders route
    this.router.navigate(['/orders'], { replaceUrl: true });
  }
}
