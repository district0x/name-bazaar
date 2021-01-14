pragma solidity ^0.4.18;

import "./ens-repo/contracts/HashRegistrarSimplified.sol";

contract NameBazaarRegistrar is Registrar {
    /**
     * @dev Constructs a new Registrar, with the provided address as the owner of the root node.
     *
     * @param ens The address of the ENS
     * @param rootNode The hash of the rootnode.
     */
    function NameBazaarRegistrar(AbstractENS ens, bytes32 rootNode, uint startDate) Registrar(ens, rootNode, startDate) public {}

    /**
     * @dev Convenience function added by NameBazaar for instant registration
     *      used for development only
     *
     * @param _hash The sha3 hash of the label to register.
     */
    function register(bytes32 _hash) payable {
        Deed bid = (new Deed).value(msg.value)(msg.sender);
        sealedBids[msg.sender][bytes32(0)] = bid;
        entry newAuction = _entries[_hash];
        newAuction.registrationDate = now - 6 days;
        newAuction.value = msg.value;
        newAuction.highestBid = msg.value;
        newAuction.deed = bid;
        trySetSubnodeOwner(_hash, msg.sender);
    }
}
