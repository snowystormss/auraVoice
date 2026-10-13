package com.example.engine

/**
 * Phonetically-balanced IEEE Harvard Sentences Corpus for TTS model conditioning,
 * acoustic alignment, and synthetic dataset generation.
 */
object HarvardSentences {

    val PREVIEW_SENTENCE: String = "The oak tree was tall and green."

    val SENTENCES: List<String> = listOf(
        // List 1
        "The oak tree was tall and green.",
        "The birch canoe slid on the smooth planks.",
        "Glue the sheet to the dark blue background.",
        "Four hours of steady work brought success.",
        "These days a chicken leg is a rare dish.",
        "Rice is often served in round bowls.",
        "The juice of lemons makes fine punch.",
        "The box was thrown beside the parked truck.",
        "The hogs were fed chopped corn and garbage.",
        "A salt pickle tastes fine with ham.",

        // List 2
        "Pass the meat and pass the bread quickly.",
        "A rod of iron is strong and heavy.",
        "He took a warm drink from the steel cup.",
        "The young colt broke through the hedge.",
        "The tiny girl wore a red ribbon in her hair.",
        "Nine small birds sat upon the wooden fence.",
        "The captain ordered the anchor dropped at dawn.",
        "Fresh green grass grew across the open hill.",
        "Cold winter rain fell upon the tin roof.",
        "A bright silver coin rolled down the street.",

        // List 3
        "The clock struck twelve in the quiet house.",
        "A white sail moved across the clear lake.",
        "The old barn stood against the autumn wind.",
        "Fresh baked bread smells delicious in the kitchen.",
        "A sudden flash of lightning illuminated the valley.",
        "The deep forest was filled with singing birds.",
        "He opened the heavy iron gate with both hands.",
        "The red sunset faded behind the mountain ridge.",
        "Clean water flowed from the rocky spring.",
        "The leather shoes were polished to a mirror shine.",

        // List 4
        "She wore a woolen sweater on the cold morning.",
        "The train whistle echoed through the narrow canyon.",
        "Dry autumn leaves rustled on the gravel path.",
        "The stone bridge arched high over the river.",
        "A gentle breeze shook the orchard blossoms.",
        "The library was silent except for turning pages.",
        "He carved a wooden whistle with a sharp knife.",
        "The yellow school bus stopped at the crossroads.",
        "The fisherman mended his net by the harbor.",
        "A golden lamp hung from the vaulted ceiling.",

        // List 5
        "The campfire crackled under the starry sky.",
        "He carried a basket of ripe red strawberries.",
        "The mail carrier delivered the letter at noon.",
        "A brown sparrow hopped across the porch railing.",
        "The brass kettle boiled on the kitchen stove.",
        "Children laughed as they ran through the park.",
        "The wooden sailboat tilted into the brisk wind.",
        "A stack of old newspapers lay near the hearth.",
        "The distant church bells rang across the green meadow.",
        "She tied the blue bundle with sturdy twine.",

        // List 6
        "A cup of hot tea warmed his cold fingers.",
        "The farmer plowed the fertile brown field.",
        "A bright rainbow appeared after the sudden storm.",
        "The bakery window was filled with fresh pastries.",
        "He counted the copper pennies on the wooden table.",
        "The lighthouse beacon cut through the dense fog.",
        "A little black kitten slept in the sunshine.",
        "The silver pocket watch ticked steadily on the desk.",
        "Wild geese flew southward in a neat formation.",
        "The artist painted the rolling ocean waves.",

        // List 7
        "Ripe red apples fell upon the soft orchard moss.",
        "The engine hummed softly as the car accelerated.",
        "A cool drink of lemonade refreshed the hikers.",
        "The heavy wooden trunk was locked with an iron key.",
        "Morning dew glistened on the spiderweb.",
        "The village market opened early on Saturday.",
        "He sharpened his pencil before taking notes.",
        "The river bank was lined with weeping willows.",
        "A flock of white gulls circled the fishing vessel.",
        "She stitched the colorful pattern with silk thread.",

        // List 8
        "The garden path was paved with flat grey stones.",
        "A swift hawk soared high above the pine trees.",
        "He folded the clean linen napkins neatly.",
        "The grandfather clock chimed every quarter hour.",
        "A row of potted geraniums lined the windowsill.",
        "The smell of cedar wood filled the workshop.",
        "Morning sunlight streamed through the tall stained glass.",
        "The little stream tumbled over smooth round pebbles.",
        "He tightened the loose bolt with an iron wrench.",
        "A flock of sheep grazed along the green hillside.",

        // List 9
        "The bakery was fragrant with cinnamon and yeast.",
        "A sudden gust of wind blew the map away.",
        "She gathered wildflowers along the country road.",
        "The iron skillet sizzled with bacon and eggs.",
        "The old sailor told stories of distant voyages.",
        "A thin veil of mist hung over the sleepy pond.",
        "He wrapped the crystal vase in bubble wrap.",
        "The autumn harvest was rich and bountiful.",
        "Children built a tall sandcastle near the water.",
        "The church choir sang with joyful voices.",

        // List 10
        "A brown squirrel buried acorns beneath the oak tree.",
        "The coffee maker dripped dark roast into the pot.",
        "The harbor was calm as the evening tide rolled in.",
        "She clipped fresh mint leaves from the herbal garden.",
        "A sturdy wooden ladder rested against the barn wall.",
        "The starry night was illuminated by a full silver moon.",
        "He tuned each guitar string until it sounded pitch perfect.",
        "The snow covered the fir branches with white powder.",
        "A friendly golden retriever bounded across the lawn.",
        "The candle flame flickered softly on the table.",

        // List 11
        "The wooden door creaked as it opened into the room.",
        "A basket of purple plums sat on the kitchen counter.",
        "The clear night air was cold and crisp.",
        "He pressed the iron stamp into the melted red wax.",
        "A bright red cardinal sang from the dogwood branch.",
        "The stone fireplace warmed the cozy cabin.",
        "She poured olive oil into the ceramic dish.",
        "The ocean waves crashed rhythmically against the breakwater.",
        "A flock of wild turkeys crossed the gravel road.",
        "The old leather atlas showed continents and sea routes.",

        // List 12
        "He checked the tire pressure before the road trip.",
        "A field of yellow sunflowers faced the morning sun.",
        "The kettle whistled loudly on the copper burner.",
        "She braided her sister's hair with satin ribbon.",
        "The distant thunder warned of a coming summer storm.",
        "A sleek grey cat stretched out on the warm radiator.",
        "The carpenter measured the pine beam twice.",
        "Sweet peach preserves were spread on warm biscuits.",
        "The wooden dock rocked gently with the lake water.",
        "He signed the deed with an antique fountain pen.",

        // List 13
        "A basket of golden pears ripened on the veranda.",
        "The steam rose gently from the bowl of soup.",
        "The mountain air was thin and refreshingly pure.",
        "She found a smooth sea glass pebble on the beach.",
        "The blue jays chattered in the tall maple tree.",
        "The train rounded the curve with a steady roar.",
        "A silver ladle rested inside the soup tureen.",
        "He stacked the split firewood beside the log cabin.",
        "The morning fog lifted to reveal the distant island.",
        "Children skipped stones across the glass-like water.",

        // List 14
        "A fragrant bunch of lavender dried in the pantry.",
        "The grandfather rocking chair swayed back and forth.",
        "He adjusted the microscope focus with precision.",
        "The autumn breeze scattered amber leaves across the grass.",
        "A bowl of fresh cherries made a sweet afternoon snack.",
        "The copper weather vane pointed directly east.",
        "She wrapped the warm woolen scarf around her neck.",
        "The river ferry carried cars and passengers across.",
        "A family of deer grazed peacefully at the edge of the woods.",
        "The antique clock had a brass pendulum that swung smoothly.",

        // List 15
        "The aroma of roasting coffee drifted down the street.",
        "He tied a secure square knot at the end of the rope.",
        "A silver airplane left a white vapor trail in the blue sky.",
        "The honey bees buzzed around the clover blossoms.",
        "She selected a ripe melon from the farmer's stall.",
        "The stone walkway was lined with solar lanterns.",
        "A friendly greeting welcomed visitors to the museum.",
        "The rain tapped gently on the bedroom window pane.",
        "He cleaned the telescope lens before the star show.",
        "The campfire embers glowed orange in the dark.",

        // List 16
        "A pitcher of iced water condensed on the outside.",
        "The young sapling was supported by wooden stakes.",
        "She arranged colorful dahlias in a crystal vase.",
        "The violin soloist played a captivating melody.",
        "He carved his initials into the old walking stick.",
        "A gentle snowfall blanketed the quiet neighborhood.",
        "The wooden fence was whitewashed every spring.",
        "Fresh peppermint tea was brewed in a porcelain pot.",
        "The hawk dove quickly toward the grassy knoll.",
        "A stack of cedar shingles lay on the roof.",

        // List 17
        "The morning paper was tossed onto the front stoop.",
        "She sprinkled cinnamon sugar over the warm donut.",
        "The old mill wheel turned slowly in the creek.",
        "A cluster of blue hydrangeas bloomed near the porch.",
        "He tested the circuit with a digital multimeter.",
        "The desert sky turned crimson as twilight approached.",
        "A bowl of salted almonds sat on the side table.",
        "The little girl skipped happily along the boardwalk.",
        "He tuned the radio to the classical music station.",
        "The forest trail climbed steadily toward the summit.",

        // List 18
        "A dish of vanilla ice cream melted in the summer heat.",
        "The blacksmith hammered red iron on the anvil.",
        "She wrapped the birthday gift in colorful foil paper.",
        "The boat glided silently through the dark marshland.",
        "He tightened the strap on his leather hiking backpack.",
        "The village fountain splashed water into the stone basin.",
        "A friendly barn owl roosted in the high rafters.",
        "The aroma of pine needles filled the mountain valley.",
        "She smoothed the wrinkles from the linen tablecloth.",
        "The little brook meandered through the green pasture.",

        // List 19
        "A basket of fresh walnuts was cracked by the fireplace.",
        "The sailboat leaned gracefully under the steady wind.",
        "He checked the compass heading before setting sail.",
        "The golden honey dripped slowly from the wooden dipper.",
        "A flock of swallows darted across the evening sky.",
        "The stone cathedral had towering flying buttresses.",
        "She polished the brass doorknob until it shone brightly.",
        "The morning air smelled of wet earth and pine.",
        "He pressed the shutter button to capture the sunrise.",
        "The wooden rocking horse stood in the nursery corner.",

        // List 20
        "A pot of hearty beef stew simmered on the stove.",
        "The autumn maple leaves turned brilliant shades of scarlet.",
        "He oiled the garden shears before trimming the hedge.",
        "The crystal chandelier shimmered with rainbow prisms.",
        "A pair of white swans drifted across the calm canal.",
        "She read the ancient manuscript under a magnifying glass.",
        "The small airplane touched down gently on the runway.",
        "A cluster of red grapes hung from the arbor.",
        "He fastened the brass buckle on his winter trench coat.",
        "The fireplace mantle was adorned with evergreen garland.",

        // List 21
        "The morning fog shrouded the towering suspension bridge.",
        "She kneaded the sourdough dough with rhythmic motions.",
        "The grandfather clock struck six times with deep resonance.",
        "A gentle current carried the canoe down the river.",
        "He studied the star charts with a red filtered flashlight.",
        "The village cobbler resoled the sturdy leather boots.",
        "A row of colorful beach umbrellas dotted the sand.",
        "The smell of sweet clover drifted through the open window.",
        "She folded the crisp white cotton sheets carefully.",
        "The little stream sparkled under the afternoon sun.",

        // List 22
        "A bowl of fresh raspberries was sweetened with honey.",
        "The steam engine blew its whistle before entering the tunnel.",
        "He checked the weather barometer on the hallway wall.",
        "The autumn wind blew dry leaves in spiraling eddies.",
        "A mother duck led her ducklings into the calm pond.",
        "She hung the copper pots from the iron rack.",
        "The violinist played a lively reel that delighted everyone.",
        "A plate of golden pancakes was topped with maple syrup.",
        "He marked the trail with bright yellow trail ribbons.",
        "The evening breeze carried the scent of salty sea spray.",

        // List 23
        "The wooden windmill turned steadily in the prairie wind.",
        "She picked ripe blackberries along the bramble fence.",
        "The antique pocket compass always pointed true north.",
        "A cluster of white birch trees stood beside the river.",
        "He poured warm milk into the ceramic cereal bowl.",
        "The artist mixed cobalt blue with titanium white on the palette.",
        "A red fox dashed across the snowy clearing.",
        "The garden gate swung open with a gentle squeak.",
        "She arranged freshly picked cosmos in a mason jar.",
        "The old oak desk was carved with intricate scrollwork.",

        // List 24
        "A pot of hot cocoa warmed the shivering children.",
        "The lighthouse keeper climbed the spiral staircase at dusk.",
        "He tightened the screws on the wooden picture frame.",
        "The smell of roasted chestnuts filled the winter air.",
        "A flock of sandpipers scampered along the wet beach.",
        "She traced the embroidered floral pattern with her finger.",
        "The little propeller plane circled above the airstrip.",
        "A bowl of salted popcorn was shared during the movie.",
        "He swept the fallen leaves from the cedar deck.",
        "The calm harbor mirrored the lights of the quiet town.",

        // List 25
        "The bakery was famous for its crusty sourdough boules.",
        "A sudden shower brought out the scent of wet asphalt.",
        "She placed a bookmark between the pages of the novel.",
        "The carpenter planed the cedar plank smooth as silk.",
        "A friendly golden retriever caught the flying disc in midair.",
        "The grandfather clock chimed the arrival of midnight.",
        "He gathered dry kindling from beneath the hemlock tree.",
        "The blue sky was dotted with fluffy cumulus clouds.",
        "She poured hot water into the French press coffee pot.",
        "The iron gate clicked securely into its latch.",

        // List 26
        "A basket of fresh figs was harvested from the garden tree.",
        "The night breeze made the porch wind chimes ring softly.",
        "He checked the altitude meter on the hiking watch.",
        "The autumn sunlight cast long golden shadows across the lawn.",
        "A curious chipmunk peeked out from behind the stone wall.",
        "She brushed the horse's chestnut coat until it gleamed.",
        "The wooden pier extended far into the calm bay.",
        "A stack of blueberry pancakes was served with warm butter.",
        "He adjusted the antenna for clearer reception.",
        "The peaceful night was lulled by chirping crickets.",

        // List 27
        "The scent of flowering jasmine filled the evening courtyard.",
        "She rolled out the pie crust on the floured marble counter.",
        "The old stone well had cool mountain water at its bottom.",
        "A red-tailed hawk perched atop the telephone pole.",
        "He applied beeswax polish to the antique walnut table.",
        "The mountain trail was flanked by blooming rhododendrons.",
        "A bowl of hot oatmeal was sprinkled with brown sugar and raisins.",
        "The sailboat dropped its canvas anchor in the sheltered cove.",
        "She placed the freshly baked lemon tart on the cooling rack.",
        "The children built a secret fort out of sofa cushions.",

        // List 28
        "A jug of fresh apple cider sat chilling on the back porch.",
        "The diesel locomotive pulled fifty railcars across the bridge.",
        "He leveled the tripod before taking the panoramic photograph.",
        "The gentle murmur of the brook put the camper to sleep.",
        "A pair of blue herons stood motionless in the shallow marsh.",
        "She tied her apron strings before starting dinner.",
        "The wooden workbench was organized with hand tools.",
        "A bowl of crispy tortilla chips was served with salsa verde.",
        "He turned the brass dial to tune the vintage radio.",
        "The summer thunderstorm ended with a double rainbow.",

        // List 29
        "The fragrance of blooming citrus orchards swept through the valley.",
        "She stirred the simmering tomato sauce with a wooden spoon.",
        "The antique mantle clock was wound with a brass key.",
        "A solitary fisherman cast his line into the quiet eddy.",
        "He secured the kayak to the car roof with ratcheting straps.",
        "The mountain peaks were crowned with eternal snow.",
        "A warm woolen blanket was folded across the foot of the bed.",
        "The little girl blew the fluffy white dandelion seeds into the wind.",
        "He set the ceramic mug down on the cork coaster.",
        "The evening twilight bathed the countryside in violet and gold.",

        // List 30
        "A plate of warm ginger cookies filled the kitchen with spice.",
        "The ferry captain sounded the horn as they approached the slip.",
        "He checked the alignment of the telescope using the polar star.",
        "The gentle rain nurtured the budding spring garden.",
        "A majestic bald eagle soared effortlessly on the thermals.",
        "She stitched the hem of the floral summer dress.",
        "The wooden floorboards gleamed under the skylight.",
        "A cup of chamomile tea calmed the restless mind.",
        "He locked the bicycle to the iron post with a U-lock.",
        "The final note of the symphony hung in the quiet hall."
    )

    fun getSentence(index: Int): String {
        return SENTENCES[index.coerceIn(0, SENTENCES.size - 1)]
    }

    fun getBatches(batchSize: Int = 15): List<List<String>> {
        return SENTENCES.chunked(batchSize)
    }
}
