#!/bin/bash
set -e

# Configuration
PROJECT_ID="portfolio-476219"
REGION="us-central1"
BACKEND_SERVICE="fairair-api"
FRONTEND_SERVICE="fairair-web"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== FairAir Cloud Run Deployment ===${NC}"
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo ""

# Check if gcloud is authenticated
if ! gcloud auth print-access-token &>/dev/null; then
    echo -e "${RED}Error: Not authenticated with gcloud. Run 'gcloud auth login' first.${NC}"
    exit 1
fi

# Set project
gcloud config set project $PROJECT_ID

# Function to deploy backend
deploy_backend() {
    echo -e "${YELLOW}=== Deploying Backend ===${NC}"
    
    # Create a cloudbuild.yaml for backend
    cat > /tmp/cloudbuild-backend.yaml << 'EOF'
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/fairair-api', '-f', 'backend-spring/Dockerfile', '.']
images:
  - 'gcr.io/$PROJECT_ID/fairair-api'
timeout: '1200s'
EOF
    
    # Build and push using Cloud Build
    echo "Building backend image..."
    gcloud builds submit \
        --config=/tmp/cloudbuild-backend.yaml \
        --timeout=20m \
        .
    
    # Deploy to Cloud Run
    echo "Deploying to Cloud Run..."
    gcloud run deploy $BACKEND_SERVICE \
        --image gcr.io/$PROJECT_ID/$BACKEND_SERVICE \
        --platform managed \
        --region $REGION \
        --allow-unauthenticated \
        --memory 512Mi \
        --cpu 1 \
        --min-instances 0 \
        --max-instances 10 \
        --port 8080 \
        --set-env-vars "SPRING_PROFILES_ACTIVE=prod"
    
    echo -e "${GREEN}Backend deployed successfully!${NC}"
    BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE --region $REGION --format 'value(status.url)')
    echo "Backend URL: $BACKEND_URL"
}

# Function to deploy frontend
deploy_frontend() {
    echo -e "${YELLOW}=== Deploying Frontend ===${NC}"
    
    # Create a cloudbuild.yaml for frontend
    cat > /tmp/cloudbuild-frontend.yaml << 'EOF'
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/fairair-web', '-f', 'apps-kmp/Dockerfile', '.']
images:
  - 'gcr.io/$PROJECT_ID/fairair-web'
timeout: '1800s'
options:
  machineType: 'E2_HIGHCPU_8'
EOF
    
    # Build and push using Cloud Build
    echo "Building frontend image..."
    gcloud builds submit \
        --config=/tmp/cloudbuild-frontend.yaml \
        --timeout=30m \
        .
    
    # Deploy to Cloud Run
    echo "Deploying to Cloud Run..."
    gcloud run deploy $FRONTEND_SERVICE \
        --image gcr.io/$PROJECT_ID/$FRONTEND_SERVICE \
        --platform managed \
        --region $REGION \
        --allow-unauthenticated \
        --memory 256Mi \
        --cpu 1 \
        --min-instances 0 \
        --max-instances 10 \
        --port 8080
    
    echo -e "${GREEN}Frontend deployed successfully!${NC}"
    FRONTEND_URL=$(gcloud run services describe $FRONTEND_SERVICE --region $REGION --format 'value(status.url)')
    echo "Frontend URL: $FRONTEND_URL"
}

# Function to setup custom domain
setup_domains() {
    echo -e "${YELLOW}=== Setting up Custom Domains ===${NC}"
    
    echo "Mapping api.fairair.yousef.codes to backend..."
    gcloud run domain-mappings create \
        --service $BACKEND_SERVICE \
        --domain api.fairair.yousef.codes \
        --region $REGION || echo "Domain mapping may already exist"
    
    echo "Mapping fairair.yousef.codes to frontend..."
    gcloud run domain-mappings create \
        --service $FRONTEND_SERVICE \
        --domain fairair.yousef.codes \
        --region $REGION || echo "Domain mapping may already exist"
    
    echo ""
    echo -e "${GREEN}=== DNS Configuration Required ===${NC}"
    echo "Add the following DNS records to your domain (yousef.codes):"
    echo ""
    echo "For api.fairair.yousef.codes:"
    gcloud run domain-mappings describe \
        --domain api.fairair.yousef.codes \
        --region $REGION \
        --format 'value(status.resourceRecords)' 2>/dev/null || echo "  CNAME -> ghs.googlehosted.com"
    echo ""
    echo "For fairair.yousef.codes:"
    gcloud run domain-mappings describe \
        --domain fairair.yousef.codes \
        --region $REGION \
        --format 'value(status.resourceRecords)' 2>/dev/null || echo "  CNAME -> ghs.googlehosted.com"
}

# Parse arguments
case "${1:-all}" in
    backend)
        deploy_backend
        ;;
    frontend)
        deploy_frontend
        ;;
    domains)
        setup_domains
        ;;
    all)
        deploy_backend
        deploy_frontend
        echo ""
        echo -e "${GREEN}=== Deployment Complete ===${NC}"
        echo ""
        echo "Next steps:"
        echo "1. Run './deploy.sh domains' to set up custom domains"
        echo "2. Add DNS records as instructed"
        echo "3. Wait for SSL certificates to be provisioned (can take up to 24 hours)"
        ;;
    *)
        echo "Usage: $0 [backend|frontend|domains|all]"
        echo "  backend  - Deploy only the backend API"
        echo "  frontend - Deploy only the frontend web app"
        echo "  domains  - Set up custom domain mappings"
        echo "  all      - Deploy both backend and frontend (default)"
        exit 1
        ;;
esac
