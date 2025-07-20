#!/bin/bash

# Argus Kubernetes Deployment Script
set -e

echo "🚀 Deploying Argus Banner Anomaly Detection System to Kubernetes"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed or not in PATH"
    exit 1
fi

# Check if we're connected to a cluster
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Not connected to a Kubernetes cluster"
    exit 1
fi

echo "📦 Creating namespace and secrets..."
kubectl apply -f namespace.yaml

echo "🔧 Deploying Argus Core API..."
kubectl apply -f argus-core-deployment.yaml

echo "🎨 Deploying Argus UI Scheduler..."
kubectl apply -f argus-scheduler-deployment.yaml

echo "🌐 Setting up ingress and services..."
kubectl apply -f ingress.yaml

echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/argus-core -n argus
kubectl wait --for=condition=available --timeout=300s deployment/argus-scheduler -n argus

echo "✅ Argus deployment completed successfully!"

echo ""
echo "🔍 Deployment Status:"
kubectl get pods -n argus -o wide
echo ""
kubectl get services -n argus

echo ""
echo "🌐 Access Information:"
echo "  Dashboard (NodePort): http://localhost:30081"
echo "  API (NodePort):       http://localhost:30080/api/v1/anomaly/health"
echo ""
echo "  With Ingress (add to /etc/hosts): http://argus.local"
echo ""
echo "💡 Useful Commands:"
echo "  View logs (Core):      kubectl logs -f deployment/argus-core -n argus"
echo "  View logs (Scheduler): kubectl logs -f deployment/argus-scheduler -n argus"
echo "  Scale core service:    kubectl scale deployment argus-core --replicas=3 -n argus"
echo "  Delete deployment:     kubectl delete namespace argus"