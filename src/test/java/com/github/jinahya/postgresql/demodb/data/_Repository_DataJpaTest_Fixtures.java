package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.object.randomizer.ObjectRandomizerUtils;
import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.Airport;
import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.Booking;
import com.github.jinahya.postgresql.demodb.persistence.Flight;
import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

/**
 * Persists the fixtures a {@code @DataJpaTest} needs, through the repositories, never through the entity manager.
 * <p>
 * Each method randomizes an entity with the upstream randomizer, gives it whatever it refers to (persisted the same
 * way, or passed in), and saves it through that entity's repository. The randomizers draw lower-case codes and
 * validity periods in the 2030s, so a fixture never collides with an installed row, should the H2 of a cached context
 * hold any; the views of the database are tables on the H2, so a row of {@code airplanes}, {@code airports} or
 * {@code timetable} can be saved as well.
 * <p>
 * {@link _Repository_DataJpaTest} imports it; a slice test reaches it as {@code fixtures()}.
 */
@TestComponent
class _Repository_DataJpaTest_Fixtures {

    /**
     * Returns a randomized instance of the specified class, through its upstream {@code <Entity>_Randomizer}.
     *
     * @param clazz the class to randomize.
     * @param <T>   the type of the class.
     * @return a randomized instance.
     */
    static <T> T randomized(final Class<T> clazz) {
        return ObjectRandomizerUtils.newRandomizedInstanceOf(clazz).orElseThrow();
    }

    // -----------------------------------------------------------------------------------------------------------------

    Airplane airplane() {
        return airplaneRepository.save(randomized(Airplane.class));
    }

    AirplaneData airplaneData() {
        return airplaneDataRepository.save(randomized(AirplaneData.class));
    }

    Airport airport() {
        return airportRepository.save(randomized(Airport.class));
    }

    AirportData airportData() {
        return airportDataRepository.save(randomized(AirportData.class));
    }

    Booking booking() {
        return bookingRepository.save(randomized(Booking.class));
    }

    Flight flight() {
        return flightRepository.save(randomized(Flight.class));
    }

    /**
     * Saves a randomized route between the specified airports, flown by the specified airplane.
     *
     * @param departure the airport of departure.
     * @param arrival   the airport of arrival.
     * @param airplane  the airplane.
     * @return the saved route.
     */
    Route route(final AirportData departure, final AirportData arrival, final AirplaneData airplane) {
        final var route = randomized(Route.class);
        route.setDepartureAirportData(departure);
        route.setArrivalAirportData(arrival);
        route.setAirplaneData(airplane);
        return routeRepository.save(route);
    }

    Route route() {
        return route(airportData(), airportData(), airplaneData());
    }

    /**
     * Saves a randomized seat of the specified airplane.
     *
     * @param airplane the airplane.
     * @return the saved seat; the id is derived from the airplane and the seat number, so what comes back may be a
     *         merged copy.
     */
    Seat seat(final AirplaneData airplane) {
        final var randomized = randomized(Seat.class);
        final var seat = Seat.of(airplane, randomized.getSeatNo());
        seat.setFareConditions(randomized.getFareConditions());
        return seatRepository.save(seat);
    }

    Seat seat() {
        return seat(airplaneData());
    }

    /**
     * Saves a randomized ticket of the specified booking.
     *
     * @param booking the booking.
     * @return the saved ticket.
     */
    Ticket ticket(final Booking booking) {
        final var ticket = randomized(Ticket.class);
        ticket.setBooking(booking);
        return ticketRepository.save(ticket);
    }

    Ticket ticket() {
        return ticket(booking());
    }

    /**
     * Saves a randomized segment of the specified ticket on the specified flight.
     *
     * @param ticket the ticket.
     * @param flight the flight.
     * @return the saved segment; the id is derived from the two, so what comes back may be a merged copy.
     */
    Segment segment(final Ticket ticket, final Flight flight) {
        final var randomized = randomized(Segment.class);
        final var segment = Segment.of(ticket, flight);
        segment.setFareConditions(randomized.getFareConditions());
        segment.setPrice(randomized.getPrice());
        return segmentRepository.save(segment);
    }

    Segment segment() {
        return segment(ticket(), flight());
    }

    /**
     * Saves a randomized boarding pass of the specified segment.
     *
     * @param segment the segment.
     * @return the saved boarding pass; the id is the segment's, so what comes back may be a merged copy.
     */
    BoardingPass boardingPass(final Segment segment) {
        final var randomized = randomized(BoardingPass.class);
        final var boardingPass = BoardingPass.of(segment);
        boardingPass.setSeatNo(randomized.getSeatNo());
        boardingPass.setBoardingNo(randomized.getBoardingNo());
        boardingPass.setBoardingTime(randomized.getBoardingTime());
        return boardingPassRepository.save(boardingPass);
    }

    BoardingPass boardingPass() {
        return boardingPass(segment());
    }

    Timetable timetable() {
        return timetableRepository.save(randomized(Timetable.class));
    }

    // -----------------------------------------------------------------------------------------------------------------
    @Autowired
    private AirplaneRepository airplaneRepository;

    @Autowired
    private AirplaneDataRepository airplaneDataRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AirportDataRepository airportDataRepository;

    @Autowired
    private BoardingPassRepository boardingPassRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TimetableRepository timetableRepository;
}
