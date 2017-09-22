pragma solidity ^0.4.14;

import "ens/HashRegistrarSimplified.sol";
import "OfferingRegistry.sol";
import "OfferingRequestsAbstract.sol";
import "strings.sol";

/**
 * @title OfferingFactory
 * @dev Base contract factory for creating new offerings
 */

contract OfferingFactory {
    using strings for *;

    Registrar public registrar;
    OfferingRegistry public offeringRegistry;
    OfferingRequestsAbstract public offeringRequests;
    address public emergencyMultisig;

    function OfferingFactory(
        address _registrar,
        address _offeringRegistry,
        address _offeringRequests,
        address _emergencyMultisig
    ) {
        registrar = Registrar(_registrar);
        offeringRegistry = OfferingRegistry(_offeringRegistry);
        offeringRequests = OfferingRequestsAbstract(_offeringRequests);
        emergencyMultisig = _emergencyMultisig;
    }

    /**
    * @dev Registers new offering to OfferingRegistry, clears offering requests for this ENS node
    * Must check if creator of offering is actual owner of ENS name and for top level names also deed owner
    * @param node bytes32 ENS node
    * @param labelHash bytes32 ENS labelhash
    * @param newOffering address The address of new offering
    * @param version uint The version of offering contract
    */
    function registerOffering(bytes32 node, bytes32 labelHash, address newOffering, uint version)
        internal
    {
        require(registrar.ens().owner(node) == msg.sender);
        if (node == sha3(registrar.rootNode(), labelHash)) {
            address deed;
            (,deed,,,) = registrar.entries(labelHash);
            require(Deed(deed).owner() == msg.sender);
        }

        offeringRegistry.addOffering(newOffering, node, msg.sender, version);
        offeringRequests.clearRequests(node);
    }

    /**
    * @dev Namehash function used by ENS to convert plain text name into node hash
    * @param name string Plaintext ENS name
    * @return bytes32 ENS node hash, aka node
    */
    function namehash(string name) internal returns(bytes32) {
        var nameSlice = name.toSlice();

        if (nameSlice.len() == 0) {
            return bytes32(0);
        }

        var label = nameSlice.split(".".toSlice()).toString();
        return sha3(namehash(nameSlice.toString()), sha3(label));
    }

    /**
    * @dev Calculates labelHash of ENS name
    * @param name string Plaintext ENS name
    * @return bytes32 ENS labelHash, hashed label of the name
    */
    function getLabelHash(string name) internal returns(bytes32) {
        return sha3(name.toSlice().split(".".toSlice()).toString());
    }
}