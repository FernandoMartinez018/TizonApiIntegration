#!/usr/bin/env bash
# Preparación única de la EC2 (Ubuntu Server 24.04). Ejecutar con sudo:
#   sudo bash bootstrap-ec2.sh
# Instala Docker Engine + plugin compose (repositorio oficial de Docker), utilidades, swap de 2 GB,
# zona horaria de Bogotá y, opcionalmente, ufw. NO abre puertos en AWS: eso es el Security Group.
set -euo pipefail

[ "$(id -u)" = 0 ] || { echo "Ejecutar con sudo"; exit 1; }
admin_user="${SUDO_USER:-ubuntu}"

apt-get update
apt-get install -y ca-certificates curl git jq gzip

# Docker Engine (https://docs.docker.com/engine/install/ubuntu/)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
# shellcheck disable=SC1091
codename="$(. /etc/os-release && echo "$VERSION_CODENAME")"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $codename stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
usermod -aG docker "$admin_user"

# Rotación de logs de contenedores para no llenar el disco
cat > /etc/docker/daemon.json <<'JSON'
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
JSON
systemctl restart docker

# Swap: margen para TensorFlow + JVM en 4 GB de RAM
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

timedatectl set-timezone America/Bogota
install -d -o "$admin_user" -g "$admin_user" /opt/tizon /var/log/tizon /var/backups/tizon

if [ "${ENABLE_UFW:-no}" = yes ]; then
  ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 443/udp && ufw --force enable
fi

echo "Listo. Cierra la sesión SSH y vuelve a entrar para usar docker sin sudo."
