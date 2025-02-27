import React from "react";
import PropTypes from "prop-types";

const Loader = ({ message = "Loading..." }) => {
    return (
        <div className="loader-container fade-in">
            <div className="loader-content">
                {/* Actual spinner animation; can be a CSS spinner */}
                <div className="loader"></div>
                <p className="loader-message">{message}</p>
            </div>
        </div>
    );
};

Loader.propTypes = {
    message: PropTypes.string,
};

export default Loader;
