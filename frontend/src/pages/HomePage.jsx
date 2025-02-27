import React from "react";
import { Link } from "react-router-dom";

const HomePage = () => {
    return (
        <div className="home-page fade-in">
            <div className="hero-section">
                <div className="hero-content">
                    <h1 className="hero-title">ByBud</h1>
                    <p className="hero-subtitle">Moderne leveringstjeneste med røtter i byens historie</p>

                    <div className="hero-buttons">
                        <Link to="/login" className="button button-primary">
                            Logg Inn
                        </Link>
                        <Link to="/register" className="button button-secondary">
                            Registrer
                        </Link>
                    </div>
                </div>
            </div>

            <div className="features-section">
                <div className="container">
                    <h2 className="section-title">Våre Tjenester</h2>

                    <div className="features-grid">
                        <div className="feature-card">
                            <div className="feature-icon">📦</div>
                            <h3>Rask Levering</h3>
                            <p>Vi leverer pakker raskt og effektivt til hele byen.</p>
                        </div>

                        <div className="feature-card">
                            <div className="feature-icon">🛵</div>
                            <h3>Pålitelige Bud</h3>
                            <p>Våre bud er pålitelige og profesjonelle.</p>
                        </div>

                        <div className="feature-card">
                            <div className="feature-icon">📱</div>
                            <h3>Enkel Sporing</h3>
                            <p>Følg leveringen din i sanntid med vår app.</p>
                        </div>
                    </div>
                </div>
            </div>

            <div className="cta-section">
                <div className="container">
                    <h2>Klar til å sende en pakke?</h2>
                    <p>Registrer deg nå og få en raskere leveringsopplevelse.</p>
                    <Link to="/register" className="button button-accent">
                        Kom i Gang
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default HomePage;