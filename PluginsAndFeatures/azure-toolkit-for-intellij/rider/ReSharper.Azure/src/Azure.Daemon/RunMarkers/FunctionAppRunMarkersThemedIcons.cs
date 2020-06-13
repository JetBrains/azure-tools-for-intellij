//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
//     Runtime Version:4.0.30319.42000
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace JetBrains.UI.ThemedIcons
{
	/// <summary>
	///	<para>
	///		<para>Autogenerated identifier classes and identifier objects to Themed Icons registered with <see cref="JetBrains.Application.Icons.IThemedIconManager"></see>.</para>
	///		<para>Identifier classes should be used in attributes, XAML, or generic parameters. Where an <see cref="JetBrains.UI.Icons.IconId"></see> value is expected, use the identifier object in the <c>Id</c> field of the identifier class.</para>
	///	</para>
	///</summary>
	///<remarks>
	///	<para>This code was compile-time generated to support Themed Icons in the JetBrains application.</para>
	///	<para>It has two primary goals: load the icons of this assembly to be registered with <see cref="JetBrains.Application.Icons.IThemedIconManager"></see> so that they were WPF-accessible and theme-sensitive; and emit early-bound accessors for referencing icons in codebehind in a compile-time-validated manner.</para>
	///	<h1>XAML</h1>
	///	<para>For performance reasons, the icons are not individually exposed with application resources. There is a custom markup extension to bind an image source in markup.</para>
	///	<para>To use an icon from XAML, set an <see cref="System.Windows.Media.ImageSource"></see> property to the <see cref="System.CodeDom.CodeTypeReference"></see> markup extension, which takes an icon identifier class (nested in <see cref="FunctionAppRunMarkersThemedIcons"></see> class) as a parameter.</para>
	///	<para>Example:</para>
	///	<code>&lt;Image Source="{icons:ThemedIcon myres:DodofuxThemedIconsThemedIcons+Trinity}" /&gt;</code>
	///	<h1>Attributes</h1>
	///	<para>Sometimes you have to reference an icon from a type attriute when you're defining objects in code. Typical examples are Options pages and Tool Windows.</para>
	///	<para>To avoid the use of string IDs which are not validated very well, we've emitted identifier classes to be used with <c>typeof()</c> expression, one per each icon. Use the attribute overload which takes a <see cref="System.Type"></see> for an image, and choose your icon class from nested classes in the <see cref="FunctionAppRunMarkersThemedIcons"></see> class.</para>
	///	<para>Example:</para>
	///	<code>[Item(Name="Sample", Icon=typeof(DodofuxThemedIconsThemedIcons.Trinity))]</code>
	///	<h1>CodeBehind</h1>
	///	<para>In codebehind, we have two distinct tasks: (a) specify some icon in the APIs and (b) render icon images onscreen.</para>
	///	<para>On the APIs stage you should only manipulate icon identifier objects (of type <see cref="JetBrains.UI.Icons.IconId"></see>, statically defined in <see cref="FunctionAppRunMarkersThemedIcons"></see> in <c>Id</c> fields). Icon identifier classes (nested in <see cref="FunctionAppRunMarkersThemedIcons"></see>) should be turned into icon identifier objects as early as possible. Rendering is about getting an <see cref="System.Windows.Media.ImageSource"></see> to assign to WPF, or <see cref="System.Drawing.Bitmap"></see> to use with GDI+ / Windows Forms.</para>
	///	<para>You should turn an identifier object into a rendered image as late as possible. The identifier is static and lightweight and does not depend on the current theme, while the image is themed and has to be loaded or generated/rasterized. You need an <see cref="JetBrains.Application.Icons.IThemedIconManager"></see> instance to get the image out of an icon identifier object. Once you got the image, you should take care to change it with theme changes — either by using a live image property, or by listening to the theme change event. See <see cref="JetBrains.Application.Icons.IThemedIconManager"></see> and its extensions for the related facilities.</para>
	///	<para>Example:</para>
	///	<code>// Getting IconId identifier object to use with APIs
	///IconId iconid = DodofuxThemedIconsThemedIcons.Trinity.Id;</code>
	///	<code>// Getting IconId out of an Icon Identifier Class type
	///IconId iconid = new JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsId(typeof(DodofuxThemedIconsThemedIcons.Trinity));</code>
	///	<code>// Getting image for screen rendering by IconId
	///themediconmanager.Icons[icnoid]</code>
	///	<code>// Getting image for screen rendering by Icon Identifier Class
	///themediconmanager.GetIcon&lt;DodofuxThemedIconsThemedIcons.Trinity&gt;()</code>
	///	<h1>Icons Origin</h1>
	///	<para>This code was generated by a pre-compile build task from a set of input files which are XAML files adhering to a certain convention, as convenient for exporting them from the Illustrator workspace, plus separate PNG files with raster icons. In the projects, these files are designated with <c>ThemedIconsXamlV3</c> and <c>ThemedIconPng</c> build actions and do not themselves get into the output assembly. All of such files are processed, vector images for different themes of the same icon are split and combined into the single list of icons in this assembly. This list is then written into the genearted XAML file (compiled into BAML within assembly), and serves as the source for this generated code.</para>
	///</remarks>
	public sealed class FunctionAppRunMarkersThemedIcons
	{
		#region RunFunctionApp
		[global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsAttribute()]
		public sealed class RunFunctionApp : global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsClass
		{

			/// <inheritdoc cref="RunFunctionApp">identifier class</inheritdoc>
			public static global::JetBrains.UI.Icons.IconId Id = new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsId(typeof(RunFunctionApp));

			/// <summary>Loads the image for Themed Icon RunFunctionApp theme aspect Color.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Color()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Loads the image for Themed Icon RunFunctionApp theme aspect Gray.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Gray()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Loads the image for Themed Icon RunFunctionApp theme aspect GrayDark.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_GrayDark()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Returns the set of theme images for Themed Icon RunFunctionApp.</summary>
			public override global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] GetThemeImages()
			{
				return new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] {
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Color", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Color)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Gray", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Gray)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("GrayDark", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_GrayDark))};
			}
		}
		#endregion
		
		#region RunThis
		/// <summary>
		///	<para>
		///		<para>RunThis Themed Icon generated identifiers:</para>
		///		<para>— <see cref="RunThis"></see> identifier class, for use in attributes, XAML, and generic parameters;</para>
		///		<para>— <see cref="Id"></see> identifier object, as a field in the identifier class, for use where an <see cref="JetBrains.UI.Icons.IconId"></see> value is expected.</para>
		///		<para>
		///			<code>
		///
		///       ;;;,'
		///       ++`_^+^_`
		///       **    '~+;:-
		///       \\       '"+*^'
		///       LL          .:;L=_`
		///       ??`````````````-_;r*".
		///       TT`````````````````_=L?=
		///       JJ-----------------'^*J*
		///       ||'''''''''''''':;?)!_`
		///       FF''''''''''_^*sT='
		///       CC_______,;?Cr~-
		///       [[____^\it/:`
		///       55,;TYF!_
		///       2227=.
		///
		///</code>
		///		</para>
		///	</para>
		///</summary>
		///<remarks>
		///	<para>For details on Themed Icons and their use, see Remarks on the outer class.</para>
		///</remarks>
		///<example>
		///	<code>&lt;Image Source="{icons:ThemedIcon myres:PohequkThemedIconsThemedIcons+RunThis}" /&gt;        &lt;!-- XAML --&gt;</code>
		///</example>
		///<example>
		///	<code>[Item(Name="Sample", Icon=typeof(PohequkThemedIconsThemedIcons.RunThis))]        // C# Type attribute</code>
		///</example>
		///<example>
		///	<code>IconId iconid = PohequkThemedIconsThemedIcons.RunThis.Id;        // IconId identifier object</code>
		///</example>
		///<example>
		///	<code>themediconmanager.GetIcon&lt;PohequkThemedIconsThemedIcons.RunThis&gt;()        // Icon image for rendering</code>
		///</example>
		[global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsAttribute()]
		public sealed class RunThis : global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsClass
		{

			/// <inheritdoc cref="RunThis">identifier class</inheritdoc>
			public static global::JetBrains.UI.Icons.IconId Id = new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsId(typeof(RunThis));

			/// <summary>Loads the image for Themed Icon RunThis theme aspect Color.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Color()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(@"<svg ti:v='1' width='16' height='16' viewBox='0,0,16,16' xmlns='http://www.w3.org/2000/svg' xmlns:ti='urn:schemas-jetbrains-com:tisvg'><g><path d='M0,0L16,0L16,16L0,16Z' fill='#FFFFFF' opacity='0'/><linearGradient id='F1' x1='0.5' y1='0.0064999999999999919' x2='0.5' y2='1.0135'><stop offset='0' stop-color='#38AC0E'/><stop offset='1' stop-color='#007034'/></linearGradient><path fill-rule='evenodd' d='M3,1L3,15L4.278,15L15,8.566L15,7.434L4.278,1L3,1Z' fill='url(#F1)'/><linearGradient id='F2' x1='0.5' y1='-0.015166666666666662' x2='0.5' y2='1.0075833333333331'><stop offset='0' stop-color='#8CFF63'/><stop offset='1' stop-color='#2AC672'/></linearGradient><path fill-rule='evenodd' d='M4,14L14,8L4,2L4,14Z' fill='url(#F2)'/></g></svg>");
			}

			/// <summary>Loads the image for Themed Icon RunThis theme aspect Gray.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Gray()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg("<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org" +
						"/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'><g><path d=\'M0,0L16,0L16,1" +
						"6L0,16Z\' fill=\'#FFFFFF\' opacity=\'0\'/><path d=\'M4,14L14,8L4,2L4,14Z\' fill=\'#59A86" +
						"9\'/></g></svg>");
			}

			/// <summary>Loads the image for Themed Icon RunThis theme aspect GrayDark.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_GrayDark()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg("<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org" +
						"/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'><g><path d=\'M0,0L16,0L16,1" +
						"6L0,16Z\' fill=\'#FFFFFF\' opacity=\'0\'/><path d=\'M4,14L14,8L4,2L4,14Z\' fill=\'#499C5" +
						"4\'/></g></svg>");
			}

			/// <summary>Returns the set of theme images for Themed Icon RunThis.</summary>
			public override global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] GetThemeImages()
			{
				return new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] {
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Color", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Color)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Gray", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Gray)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("GrayDark", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_GrayDark))};
			}
		}
		#endregion

		#region DebugThis
		/// <summary>
		///	<para>
		///		<para>DebugThis Themed Icon generated identifiers:</para>
		///		<para>— <see cref="DebugThis"></see> identifier class, for use in attributes, XAML, and generic parameters;</para>
		///		<para>— <see cref="Id"></see> identifier object, as a field in the identifier class, for use where an <see cref="JetBrains.UI.Icons.IconId"></see> value is expected.</para>
		///		<para>
		///			<code>
		///          "{a1_    '(a5^
		///        "{Z^`;e1__(y+-"Y5^
		///       '1aL`  `;ey*`  `+a1'
		///         ^ay;   ``   "ea\
		///   \\\\\cy1_          .*ai\\\\\
		///   aa"""":`             '""""aa
		///   aa````````````````````````aa
		///   aaaaa^````\SSSSSS\````.aaaaa
		///   aaLLL_````,;;;;;;,````-LLLaa
		///   aa````````````````````````aa
		///   aaIII~````caaaaaac````'IIIaa
		///   aa|||;----~++++++~----_|||aa
		///   aa........................aa
		///   aarrrrr+_..........'=rrrrraa
		///   """"""^vS?;:_____^\Yx!""""""
		///           '^?{YYYYY|+_
		///</code>
		///		</para>
		///	</para>
		///</summary>
		///<remarks>
		///	<para>For details on Themed Icons and their use, see Remarks on the outer class.</para>
		///</remarks>
		///<example>
		///	<code>&lt;Image Source="{icons:ThemedIcon myres:PohequkThemedIconsThemedIcons+DebugThis}" /&gt;        &lt;!-- XAML --&gt;</code>
		///</example>
		///<example>
		///	<code>[Item(Name="Sample", Icon=typeof(PohequkThemedIconsThemedIcons.DebugThis))]        // C# Type attribute</code>
		///</example>
		///<example>
		///	<code>IconId iconid = PohequkThemedIconsThemedIcons.DebugThis.Id;        // IconId identifier object</code>
		///</example>
		///<example>
		///	<code>themediconmanager.GetIcon&lt;PohequkThemedIconsThemedIcons.DebugThis&gt;()        // Icon image for rendering</code>
		///</example>
		[global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsAttribute()]
		public sealed class DebugThis : global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsClass
		{

			/// <inheritdoc cref="DebugThis">identifier class</inheritdoc>
			public static global::JetBrains.UI.Icons.IconId Id = new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsId(typeof(DebugThis));

			/// <summary>Loads the image for Themed Icon DebugThis theme aspect Color.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Color()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg("<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org" +
						"/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'><g><path d=\'M0,0L16,0L16,1" +
						"6L0,16Z\' fill=\'#FFFFFF\' opacity=\'0\'/><path fill-rule=\'evenodd\' d=\'M15,14.385L15," +
						"4.308L12.347,4.308C12.114154949744872,3.99775376586475,11.848257147593641,3.7137" +
						"494298974212,11.554,3.4610000000000003L12.89,2.143L10.719,0L9.9,0L8,1.877L6.1,0L" +
						"5.281,0L3.11,2.143L4.51,3.5189999999999997C4.240791833361822,3.7568209812411988," +
						"3.9962231740886711,4.02115614859259,3.7800000000000002,4.308L1,4.308L1,14.385L4." +
						"241,14.385C4.9044355615133295,15.068127871642393,5.7391275238630373,15.560948831" +
						"975564,6.6577335158113069,15.811893975088371C7.5763395077595765,16.0628391182011" +
						"79,8.5456604922404225,16.062839118201179,9.4642664841886912,15.811893975088371C1" +
						"0.38287247613696,15.560948831975564,11.21756443848667,15.068127871642393,11.881," +
						"14.385ZM15,14.385\' fill=\'#293D00\' opacity=\'0.74901960784313726\'/><linearGradient" +
						" id=\'F2\' x1=\'0.5\' y1=\'0\' x2=\'0.5\' y2=\'1.0000051027032557\'><stop offset=\'0\' stop-" +
						"color=\'#BBFF61\'/><stop offset=\'1\' stop-color=\'#BCD100\'/></linearGradient><path f" +
						"ill-rule=\'evenodd\' d=\'M9.977,3.613L11.466000000000001,2.143L10.308,1L8.12,3.17C8" +
						".0989999999999984,3.17,8.02,3.16,7.9999999999999991,3.16C7.9419999999999993,3.16" +
						",7.948999999999999,3.169,7.8909999999999991,3.1710000000000003L5.692,1L4.534,2.1" +
						"43L6.067,3.655C5.3364909117040122,4.029290841654487,4.72631480107167,4.602371018" +
						"2881592,4.307,5.3079999999999989L2,5.3079999999999989L2,6.923L3.753,6.923C3.7230" +
						"0187244563,7.1012843897325979,3.7046297608820238,7.2813310830559388,3.6980000000" +
						"000013,7.462L3.6980000000000013,8.538L2,8.538L2,10.154L3.7,10.154L3.7,10.692C3.7" +
						"048005678728435,11.05626599221125,3.7572462375444964,11.418342827059778,3.856000" +
						"0000000016,11.769L2,11.769L2,13.385L4.687,13.385C5.2300321490649608,14.059331369" +
						"401214,5.96295264087832,14.555628763076555,6.790689805050051,14.80951436275792C7" +
						".6184269692217823,15.063399962439284,8.50357303077822,15.063399962439284,9.33131" +
						"01949499515,14.80951436275792C10.159047359121681,14.555628763076555,10.891967850" +
						"935039,14.059331369401214,11.434999999999999,13.385L14,13.385L14,11.769L12.269,1" +
						"1.769C12.36726448907206,11.418239066076533,12.419700826147956,11.05622666203333," +
						"12.425,10.692L12.425,10.154L14,10.154L14,8.538L12.425,8.538L12.425,7.462C12.4180" +
						"39479361761,7.2813092475768668,12.39933331444052,7.10126241020991,12.36900000000" +
						"0003,6.923L14,6.923L14,5.308L11.815,5.308C11.381178971736929,4.5756159850921456," +
						"10.7420490794503,3.9862116502576312,9.977,3.6129999999999991ZM9.977,3.613\' fill=" +
						"\'url(#F2)\'/><path fill-rule=\'evenodd\' d=\'M9.8,11.5L6.2,11.5L6.2,10L9.8,10ZM9.8,1" +
						"1.5M9.8,8.5L6.2,8.5L6.2,7L9.8,7ZM9.8,8.5\' fill=\'#293D00\' opacity=\'0.749019607843" +
						"13726\'/></g></svg>");
			}

			/// <summary>Loads the image for Themed Icon DebugThis theme aspect Gray.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Gray()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg("<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org" +
						"/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'><g><path d=\'M0,0L16,0L16,1" +
						"6L0,16Z\' fill=\'#FFFFFF\' opacity=\'0\'/><path d=\'M9.977,3.613L11.466000000000001,2." +
						"143L10.308,1L8.12,3.17C8.0989999999999984,3.17,8.02,3.16,7.9999999999999991,3.16" +
						"C7.9419999999999993,3.16,7.948999999999999,3.169,7.8909999999999991,3.1710000000" +
						"000003L5.692,1L4.534,2.143L6.067,3.655C5.3364909117040122,4.029290841654487,4.72" +
						"631480107167,4.6023710182881592,4.307,5.3079999999999989L2,5.3079999999999989L2," +
						"6.923L3.753,6.923C3.72300187244563,7.1012843897325979,3.7046297608820238,7.28133" +
						"10830559388,3.6980000000000013,7.462L3.6980000000000013,8.538L2,8.538L2,10.154L3" +
						".7,10.154L3.7,10.692C3.7048005678728435,11.05626599221125,3.7572462375444964,11." +
						"418342827059778,3.8560000000000016,11.769L2,11.769L2,13.385L4.687,13.385C5.23003" +
						"21490649608,14.059331369401214,5.96295264087832,14.555628763076555,6.79068980505" +
						"0051,14.80951436275792C7.6184269692217823,15.063399962439284,8.50357303077822,15" +
						".063399962439284,9.3313101949499515,14.80951436275792C10.159047359121681,14.5556" +
						"28763076555,10.891967850935039,14.059331369401214,11.434999999999999,13.385L14,1" +
						"3.385L14,11.769L12.269,11.769C12.36726448907206,11.418239066076533,12.4197008261" +
						"47956,11.05622666203333,12.425,10.692L12.425,10.154L14,10.154L14,8.538L12.425,8." +
						"538L12.425,7.462C12.418039479361761,7.2813092475768668,12.39933331444052,7.10126" +
						"241020991,12.369000000000003,6.923L14,6.923L14,5.308L11.815,5.308C11.38117897173" +
						"6929,4.5756159850921456,10.7420490794503,3.9862116502576312,9.977,3.612999999999" +
						"9991ZM9.977,3.613M9.8,11.5L6.2,11.5L6.2,10L9.8,10ZM9.8,11.5M9.8,8.5L6.2,8.5L6.2," +
						"7L9.8,7ZM9.8,8.5\' fill=\'#59A869\'/></g></svg>");
			}

			/// <summary>Loads the image for Themed Icon DebugThis theme aspect GrayDark.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_GrayDark()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg("<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org" +
						"/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'><g><path d=\'M0,0L16,0L16,1" +
						"6L0,16Z\' fill=\'#FFFFFF\' opacity=\'0\'/><path d=\'M9.977,3.613L11.466000000000001,2." +
						"143L10.308,1L8.12,3.17C8.0989999999999984,3.17,8.02,3.16,7.9999999999999991,3.16" +
						"C7.9419999999999993,3.16,7.948999999999999,3.169,7.8909999999999991,3.1710000000" +
						"000003L5.692,1L4.534,2.143L6.067,3.655C5.3364909117040122,4.029290841654487,4.72" +
						"631480107167,4.6023710182881592,4.307,5.3079999999999989L2,5.3079999999999989L2," +
						"6.923L3.753,6.923C3.72300187244563,7.1012843897325979,3.7046297608820238,7.28133" +
						"10830559388,3.6980000000000013,7.462L3.6980000000000013,8.538L2,8.538L2,10.154L3" +
						".7,10.154L3.7,10.692C3.7048005678728435,11.05626599221125,3.7572462375444964,11." +
						"418342827059778,3.8560000000000016,11.769L2,11.769L2,13.385L4.687,13.385C5.23003" +
						"21490649608,14.059331369401214,5.96295264087832,14.555628763076555,6.79068980505" +
						"0051,14.80951436275792C7.6184269692217823,15.063399962439284,8.50357303077822,15" +
						".063399962439284,9.3313101949499515,14.80951436275792C10.159047359121681,14.5556" +
						"28763076555,10.891967850935039,14.059331369401214,11.434999999999999,13.385L14,1" +
						"3.385L14,11.769L12.269,11.769C12.36726448907206,11.418239066076533,12.4197008261" +
						"47956,11.05622666203333,12.425,10.692L12.425,10.154L14,10.154L14,8.538L12.425,8." +
						"538L12.425,7.462C12.418039479361761,7.2813092475768668,12.39933331444052,7.10126" +
						"241020991,12.369000000000003,6.923L14,6.923L14,5.308L11.815,5.308C11.38117897173" +
						"6929,4.5756159850921456,10.7420490794503,3.9862116502576312,9.977,3.612999999999" +
						"9991ZM9.977,3.613M9.8,11.5L6.2,11.5L6.2,10L9.8,10ZM9.8,11.5M9.8,8.5L6.2,8.5L6.2," +
						"7L9.8,7ZM9.8,8.5\' fill=\'#499C54\'/></g></svg>");
			}

			/// <summary>Returns the set of theme images for Themed Icon DebugThis.</summary>
			public override global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] GetThemeImages()
			{
				return new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] {
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Color", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Color)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Gray", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Gray)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("GrayDark", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_GrayDark))};
			}
		}
		#endregion

		#region Trigger
		[global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsAttribute()]
		public sealed class Trigger : global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsClass
		{

			/// <inheritdoc cref="Trigger">identifier class</inheritdoc>
			public static global::JetBrains.UI.Icons.IconId Id = new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsId(typeof(Trigger));

			/// <summary>Loads the image for Themed Icon Trigger theme aspect Color.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Color()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Loads the image for Themed Icon Trigger theme aspect Gray.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_Gray()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Loads the image for Themed Icon Trigger theme aspect GrayDark.</summary>
			public global::JetBrains.Util.Icons.TiImage Load_GrayDark()
			{
				return global::JetBrains.Util.Icons.TiImageConverter.FromTiSvg(
					"<svg ti:v=\'1\' width=\'16\' height=\'16\' viewBox=\'0,0,16,16\' xmlns=\'http://www.w3.org/2000/svg\' xmlns:ti=\'urn:schemas-jetbrains-com:tisvg\'></svg>");
			}

			/// <summary>Returns the set of theme images for Themed Icon Trigger.</summary>
			public override global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] GetThemeImages()
			{
				return new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage[] {
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Color", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Color)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("Gray", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_Gray)),
						new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.ThemedIconThemeImage("GrayDark", new global::JetBrains.Application.Icons.CompiledIconsCs.CompiledIconCsIdOwner.LoadImageDelegate(this.Load_GrayDark))};
			}
		}
		#endregion
	}
}
