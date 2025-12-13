# WireGuard VPN Server Setup Guide

This guide explains how to set up a WireGuard VPN server on Digital Ocean (or any VPS) to use with SynapseGuard VPN.

## Prerequisites

- A Digital Ocean account (or any VPS provider)
- Basic terminal knowledge

## Step 1: Create a Droplet (VPS)

1. Log in to [Digital Ocean](https://cloud.digitalocean.com)
2. Click "Create" -> "Droplets"
3. Choose:
   - **Image**: Ubuntu 22.04 LTS
   - **Plan**: Basic ($4-6/month is sufficient)
   - **Region**: Choose closest to your users (e.g., Frankfurt for EU)
   - **Authentication**: SSH keys (recommended) or password

4. Click "Create Droplet"
5. Note your server's **public IP address**

## Step 2: Install WireGuard

SSH into your server:

```bash
ssh root@YOUR_SERVER_IP
```

Update system and install WireGuard:

```bash
apt update && apt upgrade -y
apt install wireguard -y
```

## Step 3: Generate Server Keys

```bash
cd /etc/wireguard
umask 077

# Generate server private and public keys
wg genkey | tee server_private.key | wg pubkey > server_public.key

# View keys (you'll need the public key for the app)
cat server_private.key
cat server_public.key
```

**IMPORTANT**: Save the `server_public.key` - you'll need it for the app configuration.

## Step 4: Configure WireGuard Server

Create the configuration file:

```bash
nano /etc/wireguard/wg0.conf
```

Paste this configuration:

```ini
[Interface]
# Server's private key (from server_private.key)
PrivateKey = YOUR_SERVER_PRIVATE_KEY

# Server's VPN IP address
Address = 10.0.0.1/24

# WireGuard port
ListenPort = 51820

# Enable IP forwarding and NAT when interface comes up
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE

# Client peer (SynapseGuard app)
# You'll get this public key from the app
[Peer]
PublicKey = CLIENT_PUBLIC_KEY_FROM_APP
AllowedIPs = 10.0.0.2/32
```

Replace:
- `YOUR_SERVER_PRIVATE_KEY` with content of `server_private.key`
- `CLIENT_PUBLIC_KEY_FROM_APP` will be added later (see Step 6)

## Step 5: Enable IP Forwarding

```bash
# Enable IP forwarding
echo "net.ipv4.ip_forward = 1" >> /etc/sysctl.conf
sysctl -p

# Open firewall port
ufw allow 51820/udp
ufw allow OpenSSH
ufw enable
```

## Step 6: Configure the App

1. Open `src/config/servers.ts` in the project
2. Update the server configuration:

```typescript
export const VPN_SERVERS: VpnServer[] = [
  {
    id: 'do-fra1',
    name: 'Germany',
    country: 'Germany',
    countryCode: 'DE',
    city: 'Frankfurt',
    ipAddress: 'YOUR_SERVER_IP',        // Your Droplet's IP
    endpoint: 'YOUR_SERVER_IP',          // Same as above
    port: 51820,
    protocol: 'wireguard',
    latency: 25,
    load: 45,
    isPremium: false,
    flag: '\u{1F1E9}\u{1F1EA}',
    publicKey: 'YOUR_SERVER_PUBLIC_KEY', // Content of server_public.key
  },
];
```

## Step 7: Add Client to Server

When a user connects for the first time, the app generates a key pair. You need to add each client's public key to the server.

### Option A: Manual (for testing)

1. Build and run the app
2. When connecting, the app logs the client's public key
3. On the server, add the client:

```bash
# Add a new peer
wg set wg0 peer CLIENT_PUBLIC_KEY allowed-ips 10.0.0.2/32

# Make it persistent
wg-quick save wg0
```

### Option B: API-based (for production)

Create an API endpoint that:
1. Receives the client's public key
2. Assigns a unique IP (10.0.0.X)
3. Adds the peer to WireGuard
4. Returns the configuration to the client

## Step 8: Start WireGuard

```bash
# Start WireGuard
systemctl enable wg-quick@wg0
systemctl start wg-quick@wg0

# Check status
wg show
```

## Verification

On the server, check if WireGuard is running:

```bash
wg show
```

You should see:
```
interface: wg0
  public key: YOUR_SERVER_PUBLIC_KEY
  private key: (hidden)
  listening port: 51820

peer: CLIENT_PUBLIC_KEY
  allowed ips: 10.0.0.2/32
```

## Troubleshooting

### Connection fails immediately
- Check firewall: `ufw status`
- Verify port is open: `netstat -ulnp | grep 51820`
- Check WireGuard status: `systemctl status wg-quick@wg0`

### No internet after connecting
- Verify IP forwarding: `cat /proc/sys/net/ipv4/ip_forward` (should be 1)
- Check NAT rules: `iptables -t nat -L`

### Check logs
```bash
journalctl -u wg-quick@wg0 -f
```

## Adding More Servers

Repeat this process for each server location. Each server needs:
- Unique IP address
- Unique key pair
- Entry in `src/config/servers.ts`

## Security Notes

1. **Never share** your server's private key
2. **Rotate keys** periodically
3. **Use strong** SSH authentication
4. **Keep system** updated: `apt update && apt upgrade`

## Cost Estimate

| Provider | Plan | Cost/month |
|----------|------|------------|
| Digital Ocean | Basic Droplet | $4-6 |
| Vultr | Cloud Compute | $5 |
| Linode | Nanode | $5 |
| Hetzner | CX11 | ~$4 |

## Quick Reference

```bash
# Server key location
/etc/wireguard/server_public.key
/etc/wireguard/server_private.key

# Config file
/etc/wireguard/wg0.conf

# Commands
wg show                    # Show status
wg-quick up wg0           # Start
wg-quick down wg0         # Stop
systemctl restart wg-quick@wg0  # Restart
```
