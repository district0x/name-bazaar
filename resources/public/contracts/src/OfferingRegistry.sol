pragma solidity ^0.4.11;

import "UsedByFactories.sol";

contract OfferingRegistry is UsedByFactories {

    address[] public offerings;

    event onOfferingAdded(address offering);

    function addOffering(address _offering)
        onlyFactory
    {
        offerings.push(_offering);
        onOfferingAdded(_offering);
    }
}