// SPDX-License-Identifier: MIT
pragma solidity ^0.8.4;

/**
 * @title AuctionOffering
 * @dev Contains logic for AuctionOffering. This contract will be deployed only once, while users will create
 * many instances of Forwarder via AuctionOfferingFactory, which will serve as a proxy pointing this contract.
 * This way code logic for this offering won't be duplicated on blockchain.
 */

import "./Offering.sol";

contract AuctionOffering is Offering {

    struct AuctionOffering {
        // Order here is important for gas optimisations. Must be fitting into uint265 slots.
        uint64 endTime;                               // end time of the auction
        uint64 extensionDuration;                     // If new highest bid arrives less than extensionDuration before
                                                      // auction end time, the auction will be extended by another extensionDuration
        uint128 bidCount;                             // Number of bids for this auction
        uint minBidIncrease;                          // Min. amount new bid must be higher than previous one
        address payable winningBidder;                // Address of currently winning bidder
        mapping(address => uint) pendingReturns;      // Map of pending returns for overbid bidders
    }

    AuctionOffering public auctionOffering;

    /**
     * @dev Modifier to make a function callable only when auction has no bids
     */
    modifier onlyWithoutBids() {
        require(hasNoBids());
        _;
    }

    /**
     * @dev Modifier to make a function callable only when auction has at least 1 bid
     */
    modifier onlyWithBids() {
        require(!hasNoBids());
        _;
    }

    /**
     * @dev Modifier to make a function callable only when auction end time is over
     */
    modifier onlyAfterEndTime() {
        require(block.timestamp > auctionOffering.endTime);
        _;
    }

    /**
     * @dev Modifier to make a function callable only when auction end time is not over yet
     */
    modifier onlyBeforeEndTime() {
        require(block.timestamp < auctionOffering.endTime);
        _;
    }

    /**
     * @dev Constructor of auction
     * Should be callable just once, by factory
     */
    function construct(
        bytes32 _node,
        string calldata _name,
        bytes32 _labelHash,
        address payable _originalOwner,
        uint128 _version,
        uint _price,
        uint64 _endTime,
        uint64 _extensionDuration,
        uint _minBidIncrease
    ) external {
        super.construct(
            _node,
            _name,
            _labelHash,
            _originalOwner,
            _version,
            _price
        );
        doSetSettings(_endTime, _extensionDuration, _minBidIncrease);
    }

    /**
    * @dev Makes a new bid in an auction
    * Sends funds of previous winning bidder back or makes them available for withdrawal
    * Extends auction if bid happens within extensionDuration
    * Bid must be higher than minBidIncrease if other than first bid
    * Cannot be after end time of auction
    * Offering contract must own ENS name
    * Not executable when there's emergency pause
    */
    function bid()
        public
        payable
        onlyWhenNotEmergencyPaused
        onlyWhenContractIsNodeOwner
        onlyBeforeEndTime
    {
        if (auctionOffering.winningBidder == address(0x0)) {
            require(msg.value >= offering.price);
        } else {
            require(msg.value >= offering.price + auctionOffering.minBidIncrease);
            uint previousWinnerRefund = auctionOffering.pendingReturns[auctionOffering.winningBidder] + offering.price;
            if (auctionOffering.winningBidder.send(previousWinnerRefund)) {
                auctionOffering.pendingReturns[auctionOffering.winningBidder] = 0;
            } else {
                auctionOffering.pendingReturns[auctionOffering.winningBidder] = previousWinnerRefund;
            }
        }

        auctionOffering.winningBidder = payable(msg.sender);
        auctionOffering.bidCount += 1;
        offering.price = msg.value;

        if (uint(auctionOffering.endTime - auctionOffering.extensionDuration) <= block.timestamp) {
            auctionOffering.endTime = uint64(block.timestamp + uint(auctionOffering.extensionDuration));
        }

        uint[] memory extraEventData = new uint[](3);
        extraEventData[0] = uint(uint160(msg.sender));
        extraEventData[1] = offering.price;
        extraEventData[2] = block.timestamp;
        fireOnChanged("bid", extraEventData);
    }

    /**
    * @dev Withdraws pending returns of a bidder
    * Only original bidder or emergency multisig can withdraw funds,
    * In each case funds are sent to original bidder only
    * @param _address address The address of withdrawer
    */
    function withdraw(address payable _address) external {
        require(msg.sender == _address || isSenderEmergencyMultisig());
        uint pendingReturns = auctionOffering.pendingReturns[_address];
        if (pendingReturns > 0) {
            auctionOffering.pendingReturns[_address] = 0;
            _address.transfer(pendingReturns);
            fireOnChanged("withdraw");
        }
    }

    /**
    * @dev Finalizes auction: transfers funds to original ENS owner, transfers ENS name to winning bidder
    * Must be after auction end time
    * According to Withdrawal Pattern we cannot assume transferring funds to original owner will be possible,
    * therefore we try to transfer his funds, and we make them available for withdrawal later, if this transfer fails.
    */
    function finalize()
        external
        onlyAfterEndTime
        onlyWithBids
    {
        transferOwnership(auctionOffering.winningBidder);

        if (!offering.originalOwner.send(offering.price)){
            auctionOffering.pendingReturns[offering.originalOwner] =
                auctionOffering.pendingReturns[offering.originalOwner] + offering.price;
        }
    }

    /**
    * @dev Reclaims ownership of ENS name back to original owner
    * Can be done only if auction has no bids
    * Emergency multisig can do this even for auction with bids and it puts winner bidder funds for withdrawal
    */
    function reclaimOwnership() public override {
        if (isSenderEmergencyMultisig()) {
            if (!hasNoBids() && !wasEmergencyCancelled()) {
                auctionOffering.pendingReturns[auctionOffering.winningBidder] = offering.price;
            }
        } else {
            require(hasNoBids());
        }
        super.reclaimOwnership();
    }


    /**
    * @dev Changes settings for AuctionOffering
    * Can be executed only by original owner
    * Can't be executed when auction already has some bids
    * @param _startPrice uint New start price of the auction
    * @param _endTime uint New end time of the auction
    * @param _extensionDuration uint New extension duration of the auction
    * @param _minBidIncrease uint New min bid increase of the auction
    */
    function setSettings(
        uint _startPrice,
        uint64 _endTime,
        uint64 _extensionDuration,
        uint _minBidIncrease
    )
        external
        onlyOriginalOwner
        onlyWithoutBids
    {
        super.doSetSettings(_startPrice);
        doSetSettings(_endTime, _extensionDuration, _minBidIncrease);
        fireOnChanged("setSettings");
    }

    /**
    * @dev Performs actual settings change with input validation
    * Must be callable only internally
    */
    function doSetSettings(
        uint64 _endTime,
        uint64 _extensionDuration,
        uint _minBidIncrease
    )
        internal
    {
        require(_endTime <= block.timestamp + 4 * 30 days);
        require(_extensionDuration <= _endTime - block.timestamp);
        require(_endTime > block.timestamp);
        auctionOffering.endTime = _endTime;
        auctionOffering.extensionDuration = _extensionDuration;
        require(_minBidIncrease > 0);
        auctionOffering.minBidIncrease = _minBidIncrease;
    }

    /**
    * @dev Returns whether auction already has no bids
    * @return bool true if auction has no bids
    */
    function hasNoBids() public view returns(bool) {
        return auctionOffering.winningBidder == address(0x0);
    }

    /**
    * @dev Returns amount of pending returns ready for withdrawal for a given address
    * @param bidder address The address with pending returns
    * @return uint The amount of pending returns
    */
    function pendingReturns(address bidder) external view returns (uint) {
        return auctionOffering.pendingReturns[bidder];
    }

    receive() external payable {
        bid();
    }
}
