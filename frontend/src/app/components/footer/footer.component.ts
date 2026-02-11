import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  currentYear = new Date().getFullYear();

  shopLinks = [
    { label: 'Lips', path: '/products', queryParams: { category: 'lips' } },
    { label: 'Eyes', path: '/products', queryParams: { category: 'eyes' } },
    { label: 'Face', path: '/products', queryParams: { category: 'face' } },
    { label: 'Best Sellers', path: '/products', queryParams: { sort: 'popular' } },
    { label: 'New Arrivals', path: '/products', queryParams: { sort: 'newest' } },
  ];

  helpLinks = [
    { label: 'FAQs', path: '/help/faqs' },
    { label: 'Shipping', path: '/help/shipping' },
    { label: 'Returns', path: '/help/returns' },
    { label: 'Contact Us', path: '/contact' },
    { label: 'Track Order', path: '/my-orders' },
  ];

  companyLinks = [
    { label: 'About Us', path: '/about' },
    { label: 'Sustainability', path: '/sustainability' },
    { label: 'Careers', path: '/careers' },
    { label: 'Press', path: '/press' },
    { label: 'Privacy Policy', path: '/privacy' },
  ];

  socialLinks = [
    { icon: 'instagram', url: 'https://instagram.com', label: 'Instagram' },
    { icon: 'twitter', url: 'https://twitter.com', label: 'Twitter' },
    { icon: 'facebook', url: 'https://facebook.com', label: 'Facebook' },
    { icon: 'youtube', url: 'https://youtube.com', label: 'YouTube' },
  ];
}

