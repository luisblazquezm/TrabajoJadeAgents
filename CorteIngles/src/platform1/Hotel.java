/**
 * @author mrhyd
 * 
 * This class represents a hotel, which has a name and a list of rooms, 
 * 
 */

package platform1;

/**
 * @author mrhyd
 *
 */
public class Hotel {
	
	/**
	 *  Hotel name
	 */
	private final String name;
	
	/**
	 * List of rooms. Array index is room number, content is size of empty room.
	 */
	private int[] occupationCalendar;
	
	/**
	 * @param name
	 * @param occupationCalendar
	 */
	public Hotel(String name, int[] occupationCalendar) {
		super();
		this.name = name;
		this.occupationCalendar = occupationCalendar;
	}
	/**
	 * 
	 */
	public Hotel() {
		super();
		this.name = null;
		this.occupationCalendar = null;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the occupationCalendar
	 */
	public int[] getOccupationCalendar() {
		return occupationCalendar;
	}
	/**
	 * @param occupationCalendar the occupationCalendar to set
	 */
	public void setOccupationCalendar(int[] occupationCalendar) {
		this.occupationCalendar = occupationCalendar;
	}
	/**
	 * @param roomNumber Number of the room to be reserved
	 * @param numberOfCustomers Number of customers to be added
	 */
	public boolean addCustomer(int departureDate, int returnDate, int numberOfCustomers)
	{
		boolean customerCanBeAdded = true;
		
		if (departureDate == 0 || returnDate == 0 || numberOfCustomers == 0) {
			System.out.printf("%naddCustomer: invalid parameters%n");
		} else {
			for (int i = departureDate ; i <= returnDate ; i++) {
				if (this.occupationCalendar[i] < numberOfCustomers) {
					System.out.printf("%naddCustomer: room not available%n");
					customerCanBeAdded = false;
				}
			}
			
			if (customerCanBeAdded){
				System.out.printf("%nRoom available for days passed as parameters%n");
				for (int i = departureDate ; i <= returnDate ; i++) 
					this.occupationCalendar[i] -= numberOfCustomers;
					
				return true;
			}
		}
		
		return false;
		
	}
}