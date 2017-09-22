pragma solidity ^0.4.14;

/**
 * @title OfferingFactory
 * @dev Factory for creating new BuyNow offerings
 */

import "OfferingRegistry.sol";
import "OfferingFactory.sol";
import "BuyNowOffering.sol";
import "strings.sol";

contract BuyNowOfferingFactory is OfferingFactory {
    using strings for *;

    function BuyNowOfferingFactory(
        address registrar,
        address offeringRegistry,
        address offeringRequests,
        address emergencyMultisig
    )
        OfferingFactory(registrar, offeringRegistry, offeringRequests, emergencyMultisig)
    {
    }

    /**
    * @dev Deploys new BuyNow offering and registers it to OfferingRegistry
    * @param name string Plaintext ENS name
    * @param price uint The price of the offering
    */
    function createOffering(
        string name,
        uint price
    ) {
        var node = namehash(name);
        var labelHash = getLabelHash(name);
        address newOffering = new BuyNowOffering(
            offeringRegistry,
            registrar,
            node,
            name,
            getLabelHash(name),
            msg.sender,
            emergencyMultisig,
            price
        );
        registerOffering(node, labelHash, newOffering, 1); // versioning for BuyNow offerings starts at number 1
    }
}

