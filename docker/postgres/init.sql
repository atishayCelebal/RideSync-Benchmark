-- Initialize the ridesync-benchmark database
-- This script runs when the PostgreSQL container starts for the first time
-- It only sets up PostGIS extensions - Flyway will handle schema creation

-- Enable PostGIS extension for spatial data
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Log the initialization
DO $$
BEGIN
    RAISE NOTICE 'RideSync Benchmark database initialized successfully with PostGIS support';
END $$;
