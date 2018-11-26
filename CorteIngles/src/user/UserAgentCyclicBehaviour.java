package user;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import data.Activity;
import data.ActivityInformData;
import data.ActivityRequestData;
import data.Cities;
import data.Data;
import data.Hotels;
import data.InformData;
import data.ReservationInformData;
import data.ReservationRequestData;
import utilities.JadeUtils;
import utilities.PlatformUtils;
import utilities.Utils;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.ServiceDataPacket;
import messages.Messages;
import utilities.Debug;

public class UserAgentCyclicBehaviour extends CyclicBehaviour
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * To control execution
	 */
	private final int GET_INPUT_STEP = 0;
	private final int MESSAGE_STEP = 1;
	private final int RESERVATION_STEP = 0;
	private final int ACTIVITY_STEP = 1;
	private int reservationStep = this.GET_INPUT_STEP;
	private int activityStep = this.GET_INPUT_STEP;
	private int behaviourStep = this.RESERVATION_STEP;
	boolean behaviourIsOver = false;
	boolean behaviourFirstTimeOn = true;

	public UserAgentCyclicBehaviour(Agent agent)
	{
		super(agent);
	}

	@Override
	public void action()
	{
		/*
		 * Do (adInfinitum) {
		 * 		1- Make reservations' requests
		 * 		2- Show information
		 * 			2.1- If reservation not available, repeat reservation option
		 * 		3- Make activities' requests
		 * 		4- Show information
		 * 			4.1- If no activities were found, repeat reservation option
		 * }
		 */
		
		if (behaviourFirstTimeOn) {
			displayMainInterface();
			Utils.getUserInput("Pulse cualquier tecla para continuar...");
			behaviourFirstTimeOn = false;
		}
		
		switch(behaviourStep) {
			case RESERVATION_STEP: 
				processReservationRequest();
			break;
			case ACTIVITY_STEP:
				processActivityRequest();
			break;
		}
		
		if (behaviourIsOver) {
			Utils.getUserInput("Gracias por usar el servicio del Corte Ingl�s. Pulse cualquier tecla para salir...");
			this.requestPlatformShutdown();
		}
	}
	
	private void processReservationRequest() {
		
		if (reservationStep == GET_INPUT_STEP) {
			
			// Constants
			final int MAX_ATTEMPTS = 5;
			final String dateFormatString = "dd/MM/yyyy";
			final DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
			Date startLimitDate;
			Date endLimitDate;
			try {
				startLimitDate = dateFormat.parse("01/05/2019");
				endLimitDate = dateFormat.parse("31/05/2019");
			} catch (ParseException e) {
				Debug.message("UserAgentCycliBehaviour: wrong date format");
				startLimitDate = null;
				endLimitDate = null;
			}
			
			// For input
			String userWantsToTravel;
			
			// For message communication
			int numberOfServers = 0;
			ReservationRequestData reservationData = new ReservationRequestData(this.myAgent, null, null, null, null);
			ServiceDataPacket serviceDataPacket;
			
			// ---------------------------------------------------------------------------------------
	        			
			userWantsToTravel = Utils.getUserInput(
					"�Desea realizar un viaje? [S/n] (pulse 'q' si desea salir): ",
					true,
					MAX_ATTEMPTS,
					"s",
					"n",
					"q");
			 if ("qQ".contains(userWantsToTravel)) {
				 this.behaviourIsOver = true;
				 return;
			 } else if ("nN".contains(userWantsToTravel)) {
				behaviourStep = this.ACTIVITY_STEP;
				return;
		    } else if ("sS".contains(userWantsToTravel)) { // Process reservation request
	        	
	        	/*
	        	 * 1- Read input
	        	 * 2- Send message
	        	 * 3- Wait for answer
	        	 */
				
				Cities.printCityNameList(Data.getArrayOfCityNames());

				reservationData.setDestinationCity(
						Utils.getUserInput(
							"Introduzca la ciudad de destino: ",
							true,
							MAX_ATTEMPTS,
							null,
							Data.getArrayOfCityNames()
						)
				);
				
				Hotels.printHotelNameList(Data.getArrayOfHotelNames(reservationData.getDestinationCity()));
				
				reservationData.setDestinationHotel(
						Utils.getUserInput(
							"Introduzca el hotel de destino: ",
							true,
							MAX_ATTEMPTS,
							null,
							Data.getArrayOfHotelNames(reservationData.getDestinationCity())
						)
				);
				
				reservationData.setStartDate(
						Utils.getDateFromUser(
							"Introduzca la fecha de entrada ("
									+ dateFormat.format(startLimitDate)
									+ " - "
									+ dateFormat.format(endLimitDate)
									+ "): ",
							startLimitDate,
							endLimitDate,
							dateFormat,
							MAX_ATTEMPTS
						)
				);
				
				reservationData.setEndDate(
						Utils.getDateFromUser(
							"Introduzca la fecha de salida ("
									+ dateFormat.format(startLimitDate)
									+ " - "
									+ dateFormat.format(endLimitDate)
									+ "): ",
							startLimitDate,
							endLimitDate,
							dateFormat,
							MAX_ATTEMPTS
						)
				);
				
				serviceDataPacket = Messages.createReservationRequestServiceDataPacket(reservationData);
				if (serviceDataPacket == null)
					System.err.println("UserAgentCyclicBehaviour: messageContent is null");
				
				Debug.message("UserAgentCyclicBehaviour: going to send reservation REQUEST");
				numberOfServers = JadeUtils.sendMessage(
						this.myAgent,
						PlatformUtils.HANDLE_RESERVATION_SER,
						ACLMessage.REQUEST,
						serviceDataPacket
				);
				
				if (numberOfServers <= 0) {
	        		Debug.message("UserAgentCyclicBehaviour: no agents implementing requested service");
	        		return;
				} else {
					reservationStep = this.MESSAGE_STEP;
				}
			}
		}
		
		if (reservationStep == MESSAGE_STEP) {
			waitAndProcessResponse();
		}
	}

	private void processActivityRequest() {
		
		if (activityStep == GET_INPUT_STEP) {
			

			// Constants
			final int MAX_ATTEMPTS = 5;
			final String dateFormatString = "dd/MM/yyyy";
			final DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
			Date startLimitDate;
			Date endLimitDate;
			try {
				startLimitDate = dateFormat.parse("01/05/2019");
				endLimitDate = dateFormat.parse("31/05/2019");
			} catch (ParseException e) {
				Debug.message("UserAgentCycliBehaviour: wrong date format");
				startLimitDate = null;
				endLimitDate = null;
			}
			
			// For input
			String userWantsToSeeActivities;
			
			// For message communication
			int numberOfServers = 0;
			ActivityRequestData activityData = new ActivityRequestData();
			ServiceDataPacket serviceDataPacket;
			
			
			// ---------------------------------------------------------------------------------------
	        
			userWantsToSeeActivities = Utils.getUserInput(
					"�Desea ver informaci�n sobre actividades de ocio? [S/n] (pulse 'q' si desea salir): ",
					true,
					MAX_ATTEMPTS,
					"s",
					"n",
					"q");
			
			if ("qQ".contains(userWantsToSeeActivities)) {
				this.behaviourIsOver = true;
				return;
			} else if ("sS".contains(userWantsToSeeActivities)) { // Process reservation request
	        	
	        	/*
	        	 * 1- Read input
	        	 * 2- Send message
	        	 * 3- Wait for answer
	        	 */
				
				Cities.printCityNameList(Data.getArrayOfCityNames());

				activityData.setCity(
						Utils.getUserInput(
							"Introduzca la ciudad donde desea buscar actividades: ",
							true,
							MAX_ATTEMPTS,
							null,
							Data.getArrayOfCityNames()
						)
				);
							
				activityData.setStartDate(
						Utils.getDateFromUser(
							"Introduzca la fecha de inicio de la actividad ("
									+ dateFormat.format(startLimitDate)
									+ " - "
									+ dateFormat.format(endLimitDate)
									+ "): ",
							startLimitDate,
							endLimitDate,
							dateFormat,
							MAX_ATTEMPTS
						)
				);
							
				serviceDataPacket = Messages.createActivityRequestServiceDataPacket(activityData);
				if (serviceDataPacket == null)
					System.err.println("UserAgentCyclicBehaviour: messageContent is null");
				
				Debug.message("UserAgentCyclicBehaviour: going to send activity REQUEST");
				numberOfServers = JadeUtils.sendMessage(
						this.myAgent,
						PlatformUtils.HANDLE_ACTIVITY_SER,
						ACLMessage.REQUEST,
						serviceDataPacket
				);
				
				if (numberOfServers <= 0) {
	        		Debug.message("UserAgentCyclicBehaviour: no agents implementing requested service");
	        		return;
				} else {
					activityStep = this.MESSAGE_STEP;
				}
			}
		}
		
		if (activityStep == MESSAGE_STEP) {
			waitAndProcessResponse();
		}
	}
	
	
	private boolean waitAndProcessResponse() {
		
		// TODO Parse data and return true or false depending on conditions
		// Should return false if reservation is not available
		
		Debug.message("UserAgentCyclicBehaviour: waiting for INFORM message");
		
        MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		ACLMessage message = this.myAgent.receive(template);
		
        if (message == null) {
    		Debug.formattedMessage("UserAgentCyclicBehaviour: %s blocked%n", PlatformUtils.getLocalName(PlatformUtils.USER_AGENT));
    		block();
        } else {

    		Debug.message("UserAgentCyclicBehaviour: INFORM Message received");
    		
			try {
				ServiceDataPacket content = (ServiceDataPacket) message.getContentObject();
				InformData informData = (InformData) content.getData();
				
				if (informData == null)
					System.err.println("INFORM DATA NULL");
				else if (informData.getServer() == null)
					System.err.println("SERVER NULL");
				else if (PlatformUtils.getAid(PlatformUtils.RESERVATION_AGENT) == null)
					System.err.println("AID IS NULL");
				
				try {
					if (informData.getServer().equals(PlatformUtils.getAid(PlatformUtils.RESERVATION_AGENT))) {
						processReservationData((ReservationInformData) content.getData());
						reservationStep = this.GET_INPUT_STEP;
						behaviourStep = this.ACTIVITY_STEP;
					} else if (informData.getServer().equals(PlatformUtils.getAid(PlatformUtils.ACTIVITY_AGENT))) {
						processActivityData((ActivityInformData) content.getData());
						activityStep = this.GET_INPUT_STEP;
						behaviourStep = this.RESERVATION_STEP;
					} else {
						System.err.println("UserAgentCyclicBehaviour: unrecognized message's source");
					}
				} catch (NullPointerException npe) {
					System.err.println("UserAgentCyclicBehaviour: found null pointer");
					npe.printStackTrace();
				}
				
			} catch (UnreadableException e) {
				System.err.println("UserAgentCyclicBehaviour: error converting message's content");
				e.printStackTrace();
			}
    	}
        
        return true; 
	}
	
	private void processReservationData(ReservationInformData data) {
		
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		
		String[] titles = {"RESULTADO", "CIUDAD", "HOTEL", "FECHA DE ENTRADA", "FECHA DE SALIDA"};
		int[] widths = {20,12,16,16,16};
        String[][] results = new String[1][5];
        	
        if(data.isAvailable()) {
        	results[0][0] = "RESERVADO CON �XITO";
        } else {
        	results[0][0] = "NO DISPONIBLE";
        }
        results[0][1] = data.getDestinationCity();
        results[0][2] = data.getDestinationHotel();
        results[0][3] = dateFormat.format(data.getStartDate());
        results[0][4] = dateFormat.format(data.getEndDate());
        
        Utils.printStringTable(titles, widths, results);
		
	}

	/*
	private void processReservationData(ReservationInformData data) {
		// TODO Auto-generated method stub
		
		// Basically, print data
		StringBuilder sb = new StringBuilder();
		final String dateFormatString = "dd/MM/yyyy";
		final DateFormat dateFormat = new SimpleDateFormat(dateFormatString);			
		final String NEW_LINE = "\n";
        final String TABLE_JOINT_SYMBOL = "+";
        final String TABLE_V_SPLIT_SYMBOL = "|";
        final String TABLE_H_SPLIT_SYMBOL = "-";
        final String TABLE_H_SPACE_SYMBOL = " ";
        int width = 20;
        String title1 = "CITY";
        String title2 = "HOTEL";
        String title3 = "START DATE";
        String title4 = "END DATE";
        int NUM_PARAMS = 4;
        
        sb.append(NEW_LINE + TABLE_JOINT_SYMBOL);
        for (int i = 0 ; i < NUM_PARAMS ; i++){
            for (int j = 0 ; j < width ; j++)
                sb.append(TABLE_H_SPLIT_SYMBOL);
            sb.append(TABLE_JOINT_SYMBOL);
        }
        sb.append(NEW_LINE);
        

        sb.append(TABLE_V_SPLIT_SYMBOL + title1);
        for (int i = title1.length() ; i < width ; i++)
            sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + title2);
        for (int i = title2.length() ; i < width ; i++)
            sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + title3);
        for (int i = title3.length() ; i < width ; i++)
            sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + title4);
        for (int i = title4.length() ; i < width ; i++)
            sb.append(TABLE_H_SPACE_SYMBOL);
        sb.append(TABLE_V_SPLIT_SYMBOL);

        sb.append(NEW_LINE + TABLE_JOINT_SYMBOL);
        for (int i = 0 ; i < NUM_PARAMS ; i++){
            for (int j = 0 ; j < width ; j++)
                sb.append(TABLE_H_SPLIT_SYMBOL);
            sb.append(TABLE_JOINT_SYMBOL);
        }
        sb.append(NEW_LINE);
        
		String city = data.getDestinationCity();
		String hotel = data.getDestinationHotel();
		String departureDate = dateFormat.format(data.getStartDate());
		String returnDate = dateFormat.format(data.getEndDate());
		
        sb.append(TABLE_V_SPLIT_SYMBOL + city);
        for (int i = city.length() ; i < width ; i++)
        sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + hotel);
        for (int i = hotel.length() ; i < width ; i++)
        	sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + departureDate);
        for (int i = departureDate.length() ; i < width ; i++)
                sb.append(TABLE_H_SPACE_SYMBOL);
        
        sb.append(TABLE_V_SPLIT_SYMBOL + returnDate);
        for (int i = returnDate.length() ; i < width ; i++)
                sb.append(TABLE_H_SPACE_SYMBOL);
        sb.append(TABLE_V_SPLIT_SYMBOL);
    	
        
        sb.append(NEW_LINE + TABLE_JOINT_SYMBOL);
        for (int i = 0 ; i < NUM_PARAMS ; i++){
            for (int j = 0 ; j < width ; j++)
                sb.append(TABLE_H_SPLIT_SYMBOL);
            sb.append(TABLE_JOINT_SYMBOL);
        }
        sb.append(NEW_LINE);
            
        System.out.printf(sb.toString());
		
	}
	*/
	
	private void processActivityData(ActivityInformData data) {

		String[] titles = {"ACTIVIDAD", "CIUDAD", "FECHA DE ENTRADA", "FECHA DE SALIDA"};
		int[] widths = {25, 15, 15, 15};
        
        List<Activity> results = data.getListOfActivities();
        
        String[][] tableData = new String[results.size()][4];
        
        for (int i = 0; i < results.size(); ++i) {
        	
        	Activity activity = results.get(i);
        	int[] scheduleDescription = activity.getScheduleDescription();
        	
        	tableData[i][PlatformUtils.RECEIVER_ACTIVITY_CITY_INDEX] = data.getCityName();
        	tableData[i][PlatformUtils.RECEIVER_ACTIVITY_INDEX] = activity.getName();
        	tableData[i][PlatformUtils.RECEIVER_START_OF_ACTIVITY_INDEX] = String.format("%d", scheduleDescription[0]);
        	tableData[i][PlatformUtils.RECEIVER_END_OF_ACTIVITY_INDEX] = String.format("%d", scheduleDescription[1]);
        	
        }
        
        Utils.printStringTable(titles, widths, tableData);
        
	}
	
	private void displayMainInterface() {
		
		String newLine = String.format("%n");
		String mainInterface = "********************************************************************************" + newLine
				             + "*   ________                                                                   *" + newLine
				             + "*  /   _____|                                                                  *" + newLine
				             + "*  |  /        _______   _____   _______   _____                               *" + newLine
				             + "*  |  |       /   _   \\ /  _  | |__   __| |  ___|                              *" + newLine
				             + "*  |  |       |  / \\  | | |_|/     | |    |  \\                                 *" + newLine
				             + "*  |  \\_____  |  \\_/  | |    \\     | |    |  /__                               *" + newLine
				             + "*  \\________| \\_______/ |_|\\__|    |_|    |_____|                              *" + newLine
				             + "*                                                                              *" + newLine
				             + "*                                                                              *" + newLine
				             + "*                                                                              *" + newLine
				             + "*                        _________                     _                       *" + newLine
				             + "*                       |___   ___|                   | |     /                *" + newLine
				             + "*                           | |      _  __    _____   | |  _____   _____       *" + newLine
				             + "*                           | |     | |/  \\  /  _  \\  | | |  ___| |   __|      *" + newLine
				             + "*                           | |     |  __  | | /_\\ |  | | |  \\     \\  \\        *" + newLine
				             + "*                        ___| |___  | /  \\ | \\___  |  | | |  /__   _\\  \\       *" + newLine
				             + "*                       |_________| |_|  |_| __  | |  |_| |_____| |_____|      *" + newLine
				             + "*                                            \\ \\_| |                           *" + newLine
				             + "*                                             \\____/                           *" + newLine
				             + "*                                                                              *" + newLine
				             + "********************************************************************************" + newLine
				             + "*                                                                              *" + newLine
				             + "* - Servicio de reservas de hoteles                                            *" + newLine
				             + "*                                                                              *" + newLine
				             + "* - Servicio de actividades de recreo                                          *" + newLine
				             + "*                                                                              *" + newLine
				             + "********************************************************************************";
		
		Utils.clearConsole();
		System.out.println(mainInterface);
		
	}
	
	private void requestPlatformShutdown() {
		
		ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
        myAgent.getContentManager().registerLanguage(new SLCodec());
        myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
        shutdownMessage.addReceiver(myAgent.getAMS());
        shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
        try {
            myAgent.getContentManager().fillContent(shutdownMessage, new Action(myAgent.getAID(), new ShutdownPlatform()));
            myAgent.send(shutdownMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
        
}
