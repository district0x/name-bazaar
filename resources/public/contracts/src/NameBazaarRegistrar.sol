pragma solidity ^0.4.18;

import "@ensdomains/ens/contracts/HashRegistrar.sol";

contract NameBazaarRegistrar is HashRegistrar {
    /**
     * @dev Constructs a new Registrar, with the provided address as the owner of the root node.
     *
     * @param ens The address of the ENS
     * @param rootNode The hash of the rootnode.
     */
    constructor(ENS ens, bytes32 rootNode, uint startDate) HashRegistrar(ens, rootNode, startDate) public {}

    /**
     * @dev Convenience function added by NameBazaar for instant registration
     *      used for development only
     *
     * @param _hash The sha3 hash of the label to register.
     */
    function register(bytes32 _hash) public payable {
        Deed bid = (new DeedImplementation).value(msg.value)(msg.sender);
        sealedBids[msg.sender][bytes32(0)] = bid;
        Entry storage newAuction = _entries[_hash];
        newAuction.registrationDate = now - 6 days;
        newAuction.value = msg.value;
        newAuction.highestBid = msg.value;
        newAuction.deed = bid;
        trySetSubnodeOwner(_hash, msg.sender);
    }
}
