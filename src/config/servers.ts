import type {VpnServer} from '../types/vpn';

/**
 * VPN Server Configuration
 *
 * TO MAKE VPN WORK:
 * 1. Set up a WireGuard server on Digital Ocean (or any VPS)
 * 2. Replace the placeholder values below with your actual server details:
 *    - ipAddress: Your server's public IP
 *    - endpoint: Same as ipAddress (or domain name)
 *    - publicKey: Your WireGuard server's public key
 *
 * See WIREGUARD_SETUP.md for detailed instructions.
 */

// Example configuration - REPLACE WITH YOUR REAL SERVER DETAILS
export const VPN_SERVERS: VpnServer[] = [
  {
    id: 'do-fra1',
    name: 'Germany',
    country: 'Germany',
    countryCode: 'DE',
    city: 'Frankfurt',
    ipAddress: 'YOUR_SERVER_IP', // Replace with actual IP
    endpoint: 'YOUR_SERVER_IP', // Replace with actual IP or domain
    port: 51820,
    protocol: 'wireguard',
    latency: 25,
    load: 45,
    isPremium: false,
    flag: '\u{1F1E9}\u{1F1EA}',
    publicKey: 'YOUR_SERVER_PUBLIC_KEY', // Replace with actual WireGuard public key
  },
  // Add more servers as needed
];

/**
 * Demo/Test server - connects to a test endpoint
 * This is for development testing only
 */
export const DEMO_SERVER: VpnServer = {
  id: 'demo-1',
  name: 'Demo Server',
  country: 'Test',
  countryCode: 'XX',
  city: 'Local',
  ipAddress: '10.0.0.1',
  endpoint: '10.0.0.1',
  port: 51820,
  protocol: 'wireguard',
  latency: 10,
  load: 0,
  isPremium: false,
  flag: '\u{1F3F4}',
  publicKey: 'DEMO_PUBLIC_KEY',
};

/**
 * Check if servers are properly configured
 */
export function isServerConfigured(server: VpnServer): boolean {
  return (
    server.publicKey !== 'YOUR_SERVER_PUBLIC_KEY' &&
    server.publicKey !== 'DEMO_PUBLIC_KEY' &&
    server.ipAddress !== 'YOUR_SERVER_IP' &&
    server.publicKey.length === 44 // Base64 encoded WireGuard public key length
  );
}

/**
 * Get list of available (configured) servers
 */
export function getAvailableServers(): VpnServer[] {
  return VPN_SERVERS.filter(isServerConfigured);
}

/**
 * Default client address assigned to this device
 * In production, this should be assigned by your server/API
 */
export const DEFAULT_CLIENT_ADDRESS = '10.0.0.2/32';

/**
 * Default DNS servers to use
 */
export const DEFAULT_DNS_SERVERS = ['1.1.1.1', '1.0.0.1'];

/**
 * Default allowed IPs (route all traffic through VPN)
 */
export const DEFAULT_ALLOWED_IPS = ['0.0.0.0/0', '::/0'];
